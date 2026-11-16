package com.dairyfarm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        "spring.datasource.url=jdbc:h2:mem:dfms-analytics-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ADMIN_USERNAME=analytics-admin",
        "ADMIN_PASSWORD=secure-analytics-password-123",
        "ADMIN_FARM_NAME=Analytics Farm",
        "app.upload-dir=target/analytics-test-uploads"
})
class AnalyticsApiTest {
    private static final String TEST_JWT_SECRET = Base64.getEncoder()
            .encodeToString(new java.security.SecureRandom().generateSeed(48));
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> TEST_JWT_SECRET);
    }

    @Test
    void reportsPersistedProductionCostsAndFarmScopedAnalytics() throws Exception {
        String auth = "Bearer " + json(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", "analytics-admin", "password", "secure-analytics-password-123"))))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        JsonNode breed = json(mvc.perform(get("/api/breeds").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn()).get(0);
        JsonNode animal = json(mvc.perform(post("/api/animals").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("tagNumber", "ANALYTICS-COW", "type", "COW", "gender", "FEMALE",
                                "breedUuid", breed.get("uuid").asText()))))
                .andExpect(status().isOk()).andReturn());
        String animalUuid = animal.get("uuid").asText();
        mvc.perform(patch("/api/milk/animals/{uuid}/lactation", animalUuid).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("lactating", true))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/milk/production").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("animalUuid", animalUuid, "productionDate", today.toString(),
                                "session", "MORNING", "quantity", 8))))
                .andExpect(status().isOk());

        JsonNode supplier = json(mvc.perform(post("/api/inventory/suppliers").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("name", "Analytics Supplier",
                                "type", "FEED", "status", "ACTIVE"))))
                .andExpect(status().isOk()).andReturn());
        JsonNode item = json(mvc.perform(post("/api/inventory/items").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("name", "Hay", "type", "FEED",
                                "category", "FODDER", "unit", "KG", "purchasePrice", 2, "minimumStock", 0,
                                "supplierUuid", supplier.get("uuid").asText(), "status", "ACTIVE"))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(post("/api/inventory/purchases").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("supplierUuid", supplier.get("uuid").asText(),
                                "itemUuid", item.get("uuid").asText(), "purchaseDate", today.toString(),
                                "quantity", 10, "unitPrice", 2, "invoiceNumber", "ANALYTICS-1", "initialPayment", 0))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/inventory/consumptions").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("itemUuid", item.get("uuid").asText(), "animalUuid", animalUuid,
                                "date", today.toString(), "quantity", 2))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/health/records").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("animalUuid", animalUuid, "diseaseName", "Minor injury",
                                "medicalExpense", 20, "treatmentStartDate", today.toString()))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/analytics/overview").header("Authorization", auth)
                        .param("from", today.toString()).param("to", today.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalProduction").value(8))
                .andExpect(jsonPath("$.morningProduction").value(8)).andExpect(jsonPath("$.feedCost").value(4))
                .andExpect(jsonPath("$.medicalExpense").value(20)).andExpect(jsonPath("$.expenses").value(20));
        mvc.perform(get("/api/analytics/animals").header("Authorization", auth)
                        .param("from", today.toString()).param("to", today.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].tagNumber").value("ANALYTICS-COW"))
                .andExpect(jsonPath("$[0].milkProduction").value(8))
                .andExpect(jsonPath("$[0].individualFeedCost").value(4))
                .andExpect(jsonPath("$[0].estimatedDirectContribution").value(-24));
        JsonNode otherFarm = json(mvc.perform(post("/api/farms").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Other Analytics Farm", "ownerName", "Owner"))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(get("/api/analytics/overview").header("Authorization", auth)
                        .header("X-Farm-UUID", otherFarm.get("uuid").asText())
                        .param("from", today.toString()).param("to", today.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalProduction").value(0));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }
    private String body(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }
}
