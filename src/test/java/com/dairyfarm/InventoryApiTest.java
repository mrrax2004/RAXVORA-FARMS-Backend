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
        "spring.datasource.url=jdbc:h2:mem:dfms-inventory-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ADMIN_USERNAME=inventory-admin",
        "ADMIN_PASSWORD=secure-inventory-password-123",
        "ADMIN_FARM_NAME=Inventory Farm",
        "app.upload-dir=target/inventory-test-uploads"
})
class InventoryApiTest {
    private static final String TEST_JWT_SECRET = Base64.getEncoder()
            .encodeToString(new java.security.SecureRandom().generateSeed(48));
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> TEST_JWT_SECRET);
    }

    @Test
    void managesFarmScopedInventoryPurchasesConsumptionCorrectionsAndMedicineUse() throws Exception {
        String auth = "Bearer " + json(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", "inventory-admin", "password", "secure-inventory-password-123"))))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        JsonNode supplier = json(mvc.perform(post("/api/inventory/suppliers").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Farm Feed Supply", "type", "FEED",
                                "status", "ACTIVE"))))
                .andExpect(status().isOk()).andReturn());
        String supplierUuid = supplier.get("uuid").asText();
        JsonNode feed = createItem(auth, supplierUuid, "Cattle Feed", "FEED", "KG");
        JsonNode purchase = json(mvc.perform(post("/api/inventory/purchases").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("supplierUuid", supplierUuid, "itemUuid", feed.get("uuid").asText(),
                                "purchaseDate", today.toString(), "quantity", 20, "unitPrice", 4,
                                "invoiceNumber", "FEED-100", "initialPayment", 20))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(80))
                .andExpect(jsonPath("$.outstanding").value(60)).andReturn());
        mvc.perform(post("/api/inventory/purchases").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("supplierUuid", supplierUuid, "itemUuid", feed.get("uuid").asText(),
                                "purchaseDate", today.toString(), "quantity", 20, "unitPrice", 4,
                                "invoiceNumber", "FEED-100", "initialPayment", 0))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/inventory/purchases/{uuid}/payments", purchase.get("uuid").asText())
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("paymentDate", today.toString(), "amount", 30, "paymentMethod", "CASH"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outstanding").value(30));

        JsonNode consumption = json(mvc.perform(post("/api/inventory/consumptions").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("itemUuid", feed.get("uuid").asText(), "targetGroup", "LACTATING_COW",
                                "date", today.toString(), "quantity", 2))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCost").value(8)).andReturn());
        mvc.perform(post("/api/inventory/consumptions").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("itemUuid", feed.get("uuid").asText(), "targetGroup", "LACTATING_COW",
                                "date", today.toString(), "quantity", 30))))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/inventory/consumptions/{uuid}", consumption.get("uuid").asText())
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("quantity", 3, "reason", "Corrected weigh-back"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCost").value(12));
        mvc.perform(get("/api/inventory/consumptions/{uuid}/audit", consumption.get("uuid").asText())
                        .header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].reason").value("Corrected weigh-back"));
        mvc.perform(get("/api/inventory/reports").header("Authorization", auth)
                        .param("type", "monthly-consumption").param("from", today.withDayOfMonth(1).toString())
                        .param("to", today.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].group").value("LACTATING_COW"))
                .andExpect(jsonPath("$[0].quantity").value(3));
        mvc.perform(get("/api/inventory/reports").header("Authorization", auth)
                        .param("type", "supplier-balances").param("from", today.toString()).param("to", today.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].totalCost").value(30));

        JsonNode medicine = createItem(auth, supplierUuid, "Antibiotic", "MEDICINE", "BOTTLE");
        mvc.perform(post("/api/inventory/purchases").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("supplierUuid", supplierUuid, "itemUuid", medicine.get("uuid").asText(),
                                "purchaseDate", today.toString(), "quantity", 10, "unitPrice", 3,
                                "invoiceNumber", "MED-100", "initialPayment", 0))))
                .andExpect(status().isOk());

        JsonNode breed = json(mvc.perform(get("/api/breeds").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn()).get(0);
        JsonNode animal = json(mvc.perform(post("/api/animals").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("tagNumber", "INV-COW-1", "type", "COW", "gender", "FEMALE",
                                "breedUuid", breed.get("uuid").asText()))))
                .andExpect(status().isOk()).andReturn());
        Map<String, Object> healthBody = Map.of("animalUuid", animal.get("uuid").asText(),
                "diseaseName", "Mastitis", "medicineName", "Antibiotic",
                "medicineItemUuid", medicine.get("uuid").asText(), "medicineQuantity", 2,
                "treatmentStartDate", today.toString());
        JsonNode health = json(mvc.perform(post("/api/health/records").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(healthBody)))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(get("/api/inventory/items").header("Authorization", auth).param("type", "MEDICINE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].currentStock").value(8));
        Map<String, Object> updatedHealthBody = Map.of("animalUuid", animal.get("uuid").asText(),
                "diseaseName", "Mastitis", "medicineName", "Antibiotic",
                "medicineItemUuid", medicine.get("uuid").asText(), "medicineQuantity", 3,
                "treatmentStartDate", today.toString());
        mvc.perform(put("/api/health/records/{uuid}", health.get("uuid").asText())
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(body(updatedHealthBody)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/inventory/items").header("Authorization", auth).param("type", "MEDICINE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].currentStock").value(7));

        JsonNode otherFarm = json(mvc.perform(post("/api/farms").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Another Farm", "ownerName", "Owner"))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(get("/api/inventory/items").header("Authorization", auth)
                        .header("X-Farm-UUID", otherFarm.get("uuid").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    private JsonNode createItem(String auth, String supplierUuid, String name, String type, String unit) throws Exception {
        return json(mvc.perform(post("/api/inventory/items").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", name, "type", type, "category", type,
                                "unit", unit, "purchasePrice", 4, "minimumStock", 1,
                                "supplierUuid", supplierUuid, "status", "ACTIVE"))))
                .andExpect(status().isOk()).andReturn());
    }

    private JsonNode json(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private String body(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }
}
