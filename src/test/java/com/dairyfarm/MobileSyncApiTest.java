package com.dairyfarm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dairyfarm.model.MobileSyncRecord;
import com.dairyfarm.repository.MobileSyncRecordRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        "spring.datasource.url=jdbc:h2:mem:dfms-mobile-sync-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ADMIN_USERNAME=mobile-admin",
        "ADMIN_PASSWORD=secure-mobile-password-123",
        "ADMIN_FARM_NAME=Mobile Farm",
        "app.upload-dir=target/mobile-test-uploads"
})
class MobileSyncApiTest {
    private static final String TEST_JWT_SECRET = Base64.getEncoder()
            .encodeToString(new java.security.SecureRandom().generateSeed(48));
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private MobileSyncRecordRepository syncRecords;

    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> TEST_JWT_SECRET);
    }

    @Test
    void syncsOfflineProductionIdempotentlyAndScopesSnapshotsByFarm() throws Exception {
        String auth = "Bearer " + json(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", "mobile-admin", "password", "secure-mobile-password-123"))))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        JsonNode breed = json(mvc.perform(get("/api/breeds").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn()).get(0);
        JsonNode animal = json(mvc.perform(post("/api/animals").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("tagNumber", "MOBILE-COW", "type", "COW", "gender", "FEMALE",
                                "breedUuid", breed.get("uuid").asText()))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(patch("/api/milk/animals/{uuid}/lactation", animal.get("uuid").asText())
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("lactating", true))))
                .andExpect(status().isOk());

        String deviceUuid = "51ca3f99-4c0f-46a1-a835-7e13e1de9c21";
        String clientUuid = "af899a65-d7e7-4e31-b312-9e107b8fe5f1";
        Map<String, Object> record = Map.of("clientRecordUuid", clientUuid, "type", "MILK_PRODUCTION",
                "date", today.toString(), "animalUuid", animal.get("uuid").asText(),
                "session", "MORNING", "quantity", 5);
        Map<String, Object> request = Map.of("deviceUuid", deviceUuid, "records", List.of(record));
        JsonNode first = json(mvc.perform(post("/api/mobile/sync").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("CREATED")).andReturn());
        String serverUuid = first.get("results").get(0).get("serverRecordUuid").asText();
        JsonNode replay = json(mvc.perform(post("/api/mobile/sync").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("DUPLICATE")).andReturn());
        org.junit.jupiter.api.Assertions.assertEquals(serverUuid, replay.get("results").get(0).get("serverRecordUuid").asText());
        mvc.perform(post("/api/mobile/sync").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("deviceUuid", deviceUuid, "records", List.of(
                                Map.of("clientRecordUuid", clientUuid, "type", "MILK_PRODUCTION", "date", today.toString(),
                                        "animalUuid", animal.get("uuid").asText(), "session", "MORNING", "quantity", 6))))))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/milk/production").header("Authorization", auth)
                        .param("from", today.toString()).param("to", today.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));

        JsonNode otherFarm = json(mvc.perform(post("/api/farms").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Other Mobile Farm", "ownerName", "Owner"))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(get("/api/mobile/snapshot").header("Authorization", auth)
                        .header("X-Farm-UUID", otherFarm.get("uuid").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.farmUuid").value(otherFarm.get("uuid").asText()))
                .andExpect(jsonPath("$.animals").isEmpty());
    }

    @Test
    void syncsOfflineSalesAndMultiCompanySuppliesIdempotently() throws Exception {
        JsonNode login = json(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", "mobile-admin", "password", "secure-mobile-password-123"))))
                .andExpect(status().isOk()).andReturn());
        String auth = "Bearer " + login.get("token").asText();
        String userUuid = login.get("user").get("uuid").asText();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        mvc.perform(post("/api/milk/stock/opening").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("quantity", 100, "note", "mobile sync test opening stock"))))
                .andExpect(status().isOk());
        JsonNode customer = json(mvc.perform(post("/api/milk/customers").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("name", "Mobile Sale Customer",
                                "type", "INDIVIDUAL", "startDate", today.toString(), "status", "ACTIVE"))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(put("/api/milk/default-rate").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("ratePerLitre", 48))))
                .andExpect(status().isOk());

        String deviceUuid = "9118ff5a-bcca-4f4e-b96d-6d3e963625d5";
        String saleUuid = UUID.randomUUID().toString();
        Map<String, Object> sale = Map.ofEntries(
                Map.entry("clientRecordUuid", saleUuid), Map.entry("createdByUuid", userUuid),
                Map.entry("type", "MILK_SALE"), Map.entry("date", today.toString()),
                Map.entry("saleTime", "07:15"), Map.entry("customerUuid", customer.get("uuid").asText()),
                Map.entry("invoiceNumber", "MOB-" + saleUuid), Map.entry("quantity", 5),
                Map.entry("paymentMethod", "CASH"), Map.entry("amountReceived", 240),
                Map.entry("clientEstimatedRate", 48), Map.entry("clientEstimatedAmount", 240));
        Map<String, Object> saleRequest = Map.of("deviceUuid", deviceUuid, "records", List.of(sale));
        JsonNode syncedSale = json(mvc.perform(post("/api/mobile/sync").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(saleRequest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("CREATED")).andReturn());
        MobileSyncRecord savedSaleEstimate=syncRecords.findByFarm_UuidAndDeviceUuidAndClientRecordUuid(
                UUID.fromString(login.get("user").get("farmUuid").asText()),UUID.fromString(deviceUuid),UUID.fromString(saleUuid)).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("48.00"),savedSaleEstimate.getClientEstimatedRate());
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("240.00"),savedSaleEstimate.getClientEstimatedAmount());
        mvc.perform(post("/api/mobile/sync").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(saleRequest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("DUPLICATE"));
        mvc.perform(get("/api/milk/sales").header("Authorization", auth).param("from", today.toString())
                        .param("to", today.toString()).param("customerUuid", customer.get("uuid").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].saleTime").value("07:15:00"));

        String creditSaleUuid = UUID.randomUUID().toString();
        Map<String, Object> creditSale = Map.ofEntries(
                Map.entry("clientRecordUuid", creditSaleUuid), Map.entry("createdByUuid", userUuid),
                Map.entry("type", "MILK_SALE"), Map.entry("date", today.toString()),
                Map.entry("customerUuid", customer.get("uuid").asText()),
                Map.entry("invoiceNumber", "MOB-" + creditSaleUuid), Map.entry("quantity", 2),
                Map.entry("paymentMethod", "CREDIT"), Map.entry("amountReceived", 30),
                Map.entry("clientEstimatedRate", 48), Map.entry("clientEstimatedAmount", 96));
        mvc.perform(post("/api/mobile/sync").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("deviceUuid", deviceUuid, "records", List.of(creditSale)))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("CREATED"));
        String paymentUuid = UUID.randomUUID().toString();
        Map<String, Object> payment = Map.ofEntries(
                Map.entry("clientRecordUuid", paymentUuid), Map.entry("createdByUuid", userUuid),
                Map.entry("type", "MILK_PAYMENT"), Map.entry("date", today.toString()),
                Map.entry("customerUuid", customer.get("uuid").asText()), Map.entry("quantity", 20),
                Map.entry("paymentMethod", "CASH"), Map.entry("amountReceived", 20));
        mvc.perform(post("/api/mobile/sync").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("deviceUuid", deviceUuid, "records", List.of(payment)))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("CREATED"));

        JsonNode company = json(mvc.perform(post("/api/procurement/companies").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("companyName", "Regional Dairy",
                                "companyCode", "REG-D", "companyType", "PRIVATE_DAIRY", "status", "ACTIVE"))))
                .andExpect(status().isOk()).andReturn());
        JsonNode centre = json(mvc.perform(post("/api/procurement/centres").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("companyUuid", company.get("uuid").asText(),
                                "centreName", "North Centre", "centreCode", "NORTH-1", "status", "ACTIVE"))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(post("/api/procurement/pricing/{companyUuid}", company.get("uuid").asText())
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("pricingMethod", "FAT_SNF_FORMULA", "effectiveFrom", today.toString(),
                                "baseRate", 30, "fatRatePerPercentage", 0.5, "snfRatePerPercentage", 0.1))))
                .andExpect(status().isOk());

        String supplyUuid = UUID.randomUUID().toString();
        Map<String, Object> supply = Map.ofEntries(
                Map.entry("clientRecordUuid", supplyUuid), Map.entry("createdByUuid", userUuid),
                Map.entry("type", "MILK_SUPPLY"), Map.entry("date", today.toString()),
                Map.entry("buyerUuid", centre.get("legacyBuyerUuid").asText()),
                Map.entry("session", "MORNING"), Map.entry("quantity", 5),
                Map.entry("farmFat", 4), Map.entry("farmSnf", 8.5), Map.entry("density", 1.03),
                Map.entry("sampleReference", "SAMPLE-1"), Map.entry("receiptNumber", "RECEIPT-1"),
                Map.entry("buyerFat", 4.1), Map.entry("buyerSnf", 8.4),
                Map.entry("acceptedQuantity", 4.5), Map.entry("rejectedQuantity", 0.5),
                Map.entry("rejectionReason", "Quality variance"), Map.entry("clientEstimatedRate", 32.85),
                Map.entry("clientEstimatedAmount", 164.25));
        Map<String, Object> supplyRequest = Map.of("deviceUuid", deviceUuid, "records", List.of(supply));
        JsonNode syncedSupply = json(mvc.perform(post("/api/mobile/sync").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(supplyRequest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("CREATED")).andReturn());
        mvc.perform(post("/api/mobile/sync").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(supplyRequest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("DUPLICATE"));
        mvc.perform(get("/api/procurement/supplies").header("Authorization", auth)
                        .param("from", today.toString()).param("to", today.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].rejectedQuantity").value(0.5))
                .andExpect(jsonPath("$.content[0].status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.content[0].matchedRate").value(32.89))
                .andExpect(jsonPath("$.content[0].estimatedValue").value(164.45));
        String supplyServerUuid=syncedSupply.get("results").get(0).get("serverRecordUuid").asText();
        mvc.perform(patch("/api/procurement/supplies/{uuid}/verification",supplyServerUuid)
                        .header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("status","VERIFIED","aavinFat",4.1,"aavinSnf",8.4,
                                "aavinRate",33,"acceptedQuantity",4.5,"receiptNumber","RECEIPT-1",
                                "reason","Verified against company receipt"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.finalValue").value(148.5));
        JsonNode settlement=json(mvc.perform(post("/api/procurement/settlements").header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("buyerUuid",centre.get("legacyBuyerUuid").asText(),
                                "settlementNumber","MOB-SET-1","periodStart",today.toString(),"periodEnd",today.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAcceptedQuantity").value(4.5))
                .andExpect(jsonPath("$.remainingBalance").value(148.5)).andReturn());
        mvc.perform(post("/api/procurement/settlements/{uuid}/payments",settlement.get("uuid").asText())
                        .header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("paymentDate",today.toString(),"amount",100,
                                "paymentReference","PARTIAL-1"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.remainingBalance").value(48.5))
                .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"));

        JsonNode otherCompany=json(mvc.perform(post("/api/procurement/companies").header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("companyName","Other Buyer",
                                "companyCode","OTHER-BUYER","companyType","OTHER","status","ACTIVE"))))
                .andExpect(status().isOk()).andReturn());
        JsonNode otherCentre=json(mvc.perform(post("/api/procurement/centres").header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("companyUuid",otherCompany.get("uuid").asText(),
                                "centreName","Other Centre","centreCode","OTHER-1","status","ACTIVE"))))
                .andExpect(status().isOk()).andReturn());
        String missingRateUuid=UUID.randomUUID().toString();
        Map<String,Object> noRate=Map.of("clientRecordUuid",missingRateUuid,"createdByUuid",userUuid,
                "type","MILK_SUPPLY","date",today.toString(),"buyerUuid",otherCentre.get("legacyBuyerUuid").asText(),
                "session","EVENING","quantity",3,"farmFat",4.1,"farmSnf",8.3);
        mvc.perform(post("/api/mobile/sync").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("deviceUuid",deviceUuid,"records",List.of(noRate)))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("CREATED"));
        mvc.perform(get("/api/mobile/snapshot").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.customers[0].milkRate").value(48))
                .andExpect(jsonPath("$.companies[?(@.companyName=='Regional Dairy')].companyName").isNotEmpty());
        mvc.perform(get("/api/mobile/dashboard").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.todaySold").value(7))
                .andExpect(jsonPath("$.todayCompanySupply").value(8))
                .andExpect(jsonPath("$.pendingRateVerification").value(1))
                .andExpect(jsonPath("$.companySupplyValue").value(148.5))
                .andExpect(jsonPath("$.cashCollected").value(260))
                .andExpect(jsonPath("$.creditSales").value(66))
                .andExpect(jsonPath("$.customerOutstanding").value(46));
        org.junit.jupiter.api.Assertions.assertNotNull(syncedSale.get("results").get(0).get("serverRecordUuid"));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }
    private String body(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }
}
