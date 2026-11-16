package com.dairyfarm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
        "spring.datasource.url=jdbc:h2:mem:dfms-procurement-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ADMIN_USERNAME=procurement-admin",
        "ADMIN_PASSWORD=secure-procurement-password-123",
        "ADMIN_FARM_NAME=Generic Procurement Farm"
})
class ProcurementDirectoryApiTest {
    private static final String TEST_JWT_SECRET = Base64.getEncoder()
            .encodeToString(new java.security.SecureRandom().generateSeed(48));
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry registry) { registry.add("JWT_SECRET", () -> TEST_JWT_SECRET); }

    @Test
    void companyPricingSupplyAndPaymentScheduleAreFarmScoped() throws Exception {
        String auth = login();
        mvc.perform(post("/api/milk/stock/opening").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":20,\"note\":\"procurement stock test\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.availableQuantity").value(20));
        JsonNode company = json(mvc.perform(post("/api/procurement/companies").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                                "companyName", "North Dairy", "companyCode", "NORTH-DAIRY",
                                "companyType", "PRIVATE_DAIRY", "status", "ACTIVE"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.companyType").value("PRIVATE_DAIRY")).andReturn());
        String companyUuid = company.get("uuid").asText();

        JsonNode centre = json(mvc.perform(post("/api/procurement/centres").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                                "companyUuid", companyUuid, "centreName", "North Gate",
                                "centreCode", "NORTH-GATE", "status", "ACTIVE"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.companyUuid").value(companyUuid)).andReturn());

        LocalDate effectiveFrom = LocalDate.now().minusDays(1);
        mvc.perform(post("/api/procurement/pricing/{companyUuid}", companyUuid).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"pricingMethod":"FAT_SNF_FORMULA","effectiveFrom":"%s",
                                 "baseRate":10,"fatRatePerPercentage":2,"snfRatePerPercentage":1}
                                """.formatted(effectiveFrom)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pricingMethod").value("FAT_SNF_FORMULA"));
        mvc.perform(post("/api/procurement/pricing/{companyUuid}", companyUuid).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"pricingMethod":"DAILY_FIXED_RATE","effectiveFrom":"%s","fixedRatePerLitre":30}
                                """.formatted(effectiveFrom)))
                .andExpect(status().isConflict());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/procurement/payment-schedule/{companyUuid}", companyUuid)
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("paymentCycle", "EVERY_15_DAYS", "gracePeriodDays", 2))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paymentCycle").value("EVERY_15_DAYS"));

        JsonNode supply = json(mvc.perform(post("/api/procurement/supplies").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                                "buyerUuid", centre.get("legacyBuyerUuid").asText(),
                                "supplyDate", LocalDate.now().toString(), "session", "MORNING",
                                "quantity", 10, "buyerFat", 4, "buyerSnf", 8,
                                "receiptNumber", "NORTH-001"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.matchedRate").value(26))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION")).andReturn());
        mvc.perform(get("/api/milk/stock").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.availableQuantity").value(20));

        Map<String, Object> verification = Map.of("status", "VERIFIED", "aavinFat", 4, "aavinSnf", 8,
                "aavinRate", 26, "acceptedQuantity", 10, "reason", "Receipt confirmed by company.");
        mvc.perform(patch("/api/procurement/supplies/{uuid}/verification", supply.get("uuid").asText())
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(verification)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.finalValue").value(260));
        mvc.perform(get("/api/milk/stock").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.availableQuantity").value(10));
        mvc.perform(get("/api/procurement/dashboard").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.averageFat").value(4))
                .andExpect(jsonPath("$.averageSnf").value(8));
        mvc.perform(patch("/api/procurement/supplies/{uuid}/verification", supply.get("uuid").asText())
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(verification)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/milk/stock").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.availableQuantity").value(10));

        JsonNode otherFarm = json(mvc.perform(post("/api/farms").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Separate Procurement Farm", "ownerName", "Other Owner"))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(get("/api/procurement/centres").header("Authorization", auth)
                        .header("X-Farm-UUID", otherFarm.get("uuid").asText())
                        .param("companyUuid", companyUuid))
                .andExpect(status().isNotFound());
    }

    private String login() throws Exception {
        return "Bearer " + json(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"procurement-admin\",\"password\":\"secure-procurement-password-123\"}"))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
    }
    private String body(Object value) throws Exception { return mapper.writeValueAsString(value); }
    private JsonNode json(MvcResult result) throws Exception { return mapper.readTree(result.getResponse().getContentAsString()); }
}
