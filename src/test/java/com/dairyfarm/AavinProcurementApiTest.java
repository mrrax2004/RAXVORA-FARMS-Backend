package com.dairyfarm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
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
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:dfms-aavin-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ADMIN_USERNAME=aavin-admin",
        "ADMIN_PASSWORD=secure-aavin-password-123",
        "ADMIN_FARM_NAME=Aavin Procurement Farm"
})
class AavinProcurementApiTest {
    private static final String TEST_JWT_SECRET = Base64.getEncoder()
            .encodeToString(new java.security.SecureRandom().generateSeed(48));
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry registry) { registry.add("JWT_SECRET", () -> TEST_JWT_SECRET); }

    @Test
    void ratesVerificationSettlementAndPartialPaymentsUseHistoricalSnapshots() throws Exception {
        String auth = login();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        mvc.perform(post("/api/milk/stock/opening").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("quantity", 100, "note", "Procurement stock test opening"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.availableQuantity").value(100));
        JsonNode buyer = json(mvc.perform(post("/api/aavin/buyers").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("companyName", "Aavin",
                                "centreName", "Central Collection", "centreCode", "AAVIN-CENTRAL",
                                "status", "ACTIVE"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paymentCycleDays").value(15)).andReturn());
        String buyerUuid = buyer.get("uuid").asText();

        JsonNode chart = json(mvc.perform(post("/api/aavin/rate-charts").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"buyerUuid":"%s","name":"April actual chart","effectiveFrom":"%s",
                                 "slabs":[{"fatPercentage":4.00,"snfPercentage":8.50,"ratePerLitre":40.00}]}
                                """.formatted(buyerUuid, today.minusDays(1))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.slabs[0].ratePerLitre").value(40)).andReturn());
        mvc.perform(post("/api/aavin/rate-charts").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"buyerUuid":"%s","name":"Overlapping chart","effectiveFrom":"%s",
                                 "slabs":[{"fatPercentage":4.00,"snfPercentage":8.50,"ratePerLitre":41.00}]}
                                """.formatted(buyerUuid, today)))
                .andExpect(status().isConflict());

        JsonNode supply = json(mvc.perform(post("/api/aavin/supplies").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("buyerUuid", buyerUuid,
                                "supplyDate", today.toString(), "session", "MORNING", "quantity", 100,
                                "farmFat", 4, "farmSnf", 8.5, "receiptNumber", "RCPT-100"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.matchedRate").value(40))
                .andExpect(jsonPath("$.valueIsEstimate").value(true)).andReturn());
        String supplyUuid = supply.get("uuid").asText();
        mvc.perform(post("/api/aavin/supplies").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("buyerUuid", buyerUuid,
                                "supplyDate", today.toString(), "session", "EVENING", "quantity", 100,
                                "farmFat", 4, "farmSnf", 8.5, "receiptNumber", "RCPT-100"))))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/aavin/supplies/{uuid}/rate", supplyUuid).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("ratePerLitre", 43))))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/aavin/supplies/{uuid}/rate", supplyUuid).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("ratePerLitre", 43, "reason", "Corrected to the signed Aavin chart"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.manualRate").value(43));

        mvc.perform(post("/api/aavin/supplies").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("buyerUuid", buyerUuid,
                                "supplyDate", today.toString(), "session", "EVENING", "quantity", 25,
                                "farmFat", 4.1, "farmSnf", 8.5, "receiptNumber", "RCPT-101"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RATE_UNAVAILABLE"))
                .andExpect(jsonPath("$.applicableRate").doesNotExist());

        mvc.perform(patch("/api/aavin/supplies/{uuid}/verification", supplyUuid).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status", "VERIFIED",
                                "aavinFat", 3.9, "aavinSnf", 8.4, "aavinRate", 42, "acceptedQuantity", 98,
                                "receiptNumber", "RCPT-100", "reason", "Verified against collection receipt"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fatDifference").doesNotExist())
                .andExpect(jsonPath("$.finalValue").value(4214)).andExpect(jsonPath("$.valueIsEstimate").value(false));
        mvc.perform(get("/api/milk/stock").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.availableQuantity").value(2));
        mvc.perform(get("/api/aavin/supplies/{uuid}/audit", supplyUuid).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));

        JsonNode settlement = json(mvc.perform(post("/api/aavin/settlements").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("buyerUuid", buyerUuid,
                                "settlementNumber", "SET-001", "periodStart", today.toString(), "periodEnd", today.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(100))
                .andExpect(jsonPath("$.totalAcceptedQuantity").value(98))
                .andExpect(jsonPath("$.grossMilkValue").value(4214))
                .andExpect(jsonPath("$.supplies.length()").value(1)).andReturn());
        String settlementUuid = settlement.get("uuid").asText();

        mvc.perform(post("/api/aavin/settlements/{uuid}/payments", settlementUuid).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("paymentDate", today.toString(),
                                "amount", 1000, "paymentReference", "BANK-1"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.remainingBalance").value(3214));
        mvc.perform(post("/api/aavin/settlements/{uuid}/payments", settlementUuid).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("paymentDate", today.toString(),
                                "amount", 3214, "paymentReference", "BANK-2"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.remainingBalance").value(0));
        mvc.perform(post("/api/aavin/settlements").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("buyerUuid", buyerUuid,
                                "settlementNumber", "SET-DUP", "periodStart", today.toString(), "periodEnd", today.toString()))))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/aavin/supplies/{uuid}/rate", supplyUuid).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("ratePerLitre", 45, "reason", "Late change not permitted"))))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/aavin/rate-charts/{uuid}/period", chart.get("uuid").asText())
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("effectiveUntil", today.toString(), "reason", "Close actual chart period"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.effectiveUntil").value(today.toString()));
        mvc.perform(get("/api/aavin/rate-charts/{uuid}/audit", chart.get("uuid").asText()).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(post("/api/aavin/rate-charts").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"buyerUuid":"%s","name":"Next actual chart","effectiveFrom":"%s",
                                 "slabs":[{"fatPercentage":4.00,"snfPercentage":8.50,"ratePerLitre":41.00}]}
                                """.formatted(buyerUuid, today.plusDays(1))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.effectiveFrom").value(today.plusDays(1).toString()));

        JsonNode otherFarm = json(mvc.perform(post("/api/farms").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Other Aavin Farm", "ownerName", "Other Owner"))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(get("/api/aavin/supplies/{uuid}", supplyUuid).header("Authorization", auth)
                        .header("X-Farm-UUID", otherFarm.get("uuid").asText()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/aavin/rate-charts").header("Authorization", auth)
                        .header("X-Farm-UUID", otherFarm.get("uuid").asText()).param("buyerUuid", buyerUuid))
                .andExpect(status().isNotFound());
    }

    private String login() throws Exception {
        return "Bearer " + json(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"aavin-admin\",\"password\":\"secure-aavin-password-123\"}"))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
    }
    private String body(Object value) throws Exception { return mapper.writeValueAsString(value); }
    private JsonNode json(MvcResult result) throws Exception { return mapper.readTree(result.getResponse().getContentAsString()); }
}
