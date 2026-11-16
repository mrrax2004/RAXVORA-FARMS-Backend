package com.dairyfarm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:dfms-bill-period-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ADMIN_USERNAME=bill-period-admin",
        "ADMIN_PASSWORD=secure-bill-period-password-123",
        "ADMIN_FARM_NAME=Bill Period Farm",
        "app.upload-dir=target/bill-period-test-uploads"
})
class MilkBillPeriodApiTest {
    private static final String TEST_JWT_SECRET = java.util.Base64.getEncoder()
            .encodeToString(new java.security.SecureRandom().generateSeed(48));

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry registry) { registry.add("JWT_SECRET", () -> TEST_JWT_SECRET); }

    @Test
    void generatesRollingAndCustomBillsOnlyForCompletedPeriods() throws Exception {
        String auth = "Bearer " + json(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bill-period-admin\",\"password\":\"secure-bill-period-password-123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate rollingStart = today.minusDays(8);
        LocalDate rollingEnd = rollingStart.plusDays(6);
        mvc.perform(put("/api/milk/default-rate").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("ratePerLitre", 10))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/milk/stock/opening").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("quantity", 10, "note", "Bill period test stock"))))
                .andExpect(status().isOk());

        String rollingCustomer = createCustomer(auth, "Rolling customer", rollingStart, "EVERY_7_DAYS");
        createSubscription(auth, rollingCustomer, rollingStart);
        generateDelivery(auth, rollingCustomer, rollingEnd);
        JsonNode rollingBill = json(mvc.perform(post("/api/milk/bills/generate").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("customerUuid", rollingCustomer, "periodDate", rollingStart.plusDays(4).toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingFrequency").value("EVERY_7_DAYS"))
                .andExpect(jsonPath("$.periodStart").value(rollingStart.toString()))
                .andExpect(jsonPath("$.periodEnd").value(rollingEnd.toString()))
                .andExpect(jsonPath("$.totalAmount").value(10)).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/milk/bills/{uuid}/adjustments", rollingBill.get("uuid").asText())
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("amount", 2, "reason", "Approved delivery charge correction"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(2));
        mvc.perform(get("/api/milk/bills/{uuid}", rollingBill.get("uuid").asText()).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.adjustmentAmount").value(2))
                .andExpect(jsonPath("$.amountDue").value(12)).andExpect(jsonPath("$.adjustments.length()").value(1));
        mvc.perform(post("/api/milk/payments").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("customerUuid", rollingCustomer, "billUuid", rollingBill.get("uuid").asText(),
                                "paymentDate", today.toString(), "amount", 12, "method", "CASH"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/milk/bills/generate").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("customerUuid", rollingCustomer, "periodDate", today.toString()))))
                .andExpect(status().isBadRequest());

        verifyPeriodBill(auth, "Daily customer", today, "DAILY", today, today, today, today);
        LocalDate previousMonthStart = today.withDayOfMonth(1).minusMonths(1);
        LocalDate previousMonthEnd = previousMonthStart.withDayOfMonth(1).plusMonths(1).minusDays(1);
        verifyPeriodBill(auth, "Monthly customer", previousMonthStart, "MONTHLY", previousMonthStart,
                previousMonthStart, previousMonthEnd, previousMonthEnd);
        verifyPeriodBill(auth, "First half customer", previousMonthStart, "FIRST_HALF_MONTH", previousMonthStart,
                previousMonthStart, previousMonthStart.withDayOfMonth(15), previousMonthStart.withDayOfMonth(15));
        verifyPeriodBill(auth, "Second half customer", previousMonthStart, "SECOND_HALF_MONTH",
                previousMonthStart.withDayOfMonth(16), previousMonthStart.withDayOfMonth(16), previousMonthEnd,
                previousMonthEnd);
        LocalDate fifteenDayStart = today.minusDays(20);
        verifyPeriodBill(auth, "Fifteen day customer", fifteenDayStart, "EVERY_15_DAYS", fifteenDayStart.plusDays(4),
                fifteenDayStart, fifteenDayStart.plusDays(14), fifteenDayStart.plusDays(14));
        LocalDate thirtyDayStart = today.minusDays(35);
        verifyPeriodBill(auth, "Thirty day customer", thirtyDayStart, "EVERY_30_DAYS", thirtyDayStart.plusDays(10),
                thirtyDayStart, thirtyDayStart.plusDays(29), thirtyDayStart.plusDays(29));

        LocalDate customStart = today.minusDays(3);
        LocalDate customEnd = today.minusDays(1);
        String customCustomer = createCustomer(auth, "Custom customer", customStart, "CUSTOM_PERIOD");
        createSubscription(auth, customCustomer, customStart);
        generateDelivery(auth, customCustomer, customEnd);
        mvc.perform(post("/api/milk/bills/generate").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("customerUuid", customCustomer,
                                "periodStart", customStart.toString(), "periodEnd", customEnd.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingFrequency").value("CUSTOM_PERIOD"))
                .andExpect(jsonPath("$.periodStart").value(customStart.toString()))
                .andExpect(jsonPath("$.periodEnd").value(customEnd.toString()))
                .andExpect(jsonPath("$.totalAmount").value(10));
        mvc.perform(post("/api/milk/bills/generate").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("customerUuid", customCustomer,
                                "periodStart", customEnd.toString(), "periodEnd", customStart.toString()))))
                .andExpect(status().isBadRequest());
    }

    private String createCustomer(String auth, String name, LocalDate start, String frequency) throws Exception {
        return json(mvc.perform(post("/api/milk/customers").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", name, "type", "INDIVIDUAL", "startDate", start.toString(),
                                "status", "ACTIVE", "billingFrequency", frequency))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("uuid").asText();
    }

    private void createSubscription(String auth, String customerUuid, LocalDate start) throws Exception {
        mvc.perform(post("/api/milk/subscriptions").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("customerUuid", customerUuid, "dailyQuantity", 1,
                                "session", "MORNING", "startDate", start.toString(), "unitPrice", 10))))
                .andExpect(status().isOk());
    }

    private void generateDelivery(String auth, String customerUuid, LocalDate deliveryDate) throws Exception {
        mvc.perform(post("/api/milk/deliveries/generate").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("date", deliveryDate.toString()))))
                .andExpect(status().isOk());
        JsonNode rows = json(mvc.perform(get("/api/milk/deliveries").header("Authorization", auth)
                        .param("from", deliveryDate.toString()).param("to", deliveryDate.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("content");
        String deliveryUuid = null;
        for (JsonNode row : rows) {
            if (row.get("customerUuid").asText().equals(customerUuid)) deliveryUuid = row.get("uuid").asText();
        }
        if (deliveryUuid == null) throw new AssertionError("Expected generated delivery for " + customerUuid);
        mvc.perform(patch("/api/milk/deliveries/{uuid}", deliveryUuid).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status", "DELIVERED",
                                "deliveredQuantity", 1, "notes", "Billing period test delivery"))))
                .andExpect(status().isOk());
    }

    private void verifyPeriodBill(String auth, String name, LocalDate customerStart, String frequency,
            LocalDate referenceDate, LocalDate expectedStart, LocalDate expectedEnd, LocalDate deliveryDate) throws Exception {
        String customerUuid = createCustomer(auth, name, customerStart, frequency);
        createSubscription(auth, customerUuid, customerStart);
        generateDelivery(auth, customerUuid, deliveryDate);
        JsonNode bill = json(mvc.perform(post("/api/milk/bills/generate").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("customerUuid", customerUuid, "periodDate", referenceDate.toString()))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals(frequency, bill.get("billingFrequency").asText());
        assertEquals(expectedStart.toString(), bill.get("periodStart").asText());
        assertEquals(expectedEnd.toString(), bill.get("periodEnd").asText());
        JsonNode repeated = json(mvc.perform(post("/api/milk/bills/generate").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("customerUuid", customerUuid, "periodDate", referenceDate.toString()))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals(bill.get("uuid").asText(), repeated.get("uuid").asText());
    }

    private String body(Object value) throws Exception { return mapper.writeValueAsString(value); }
    private JsonNode json(String value) throws Exception { return mapper.readTree(value); }
}
