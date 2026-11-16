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
        "spring.datasource.url=jdbc:h2:mem:dfms-finance-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ADMIN_USERNAME=finance-admin",
        "ADMIN_PASSWORD=secure-finance-password-123",
        "ADMIN_FARM_NAME=Finance Farm",
        "app.upload-dir=target/finance-test-uploads"
})
class FinanceApiTest {
    private static final String TEST_JWT_SECRET = Base64.getEncoder()
            .encodeToString(new java.security.SecureRandom().generateSeed(48));
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> TEST_JWT_SECRET);
    }

    @Test
    void managesAccountsLedgerRecurringTransactionsReportsAndFarmIsolation() throws Exception {
        String auth = "Bearer " + json(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", "finance-admin", "password", "secure-finance-password-123"))))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        JsonNode account = json(mvc.perform(post("/api/finance/accounts").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Operating Bank", "type", "BANK", "openingBalance", 1000))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(1000)).andReturn());
        String accountUuid = account.get("uuid").asText();

        mvc.perform(post("/api/finance/transactions").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("accountUuid", accountUuid, "type", "INCOME",
                                "category", "OTHER_INCOME", "date", today.toString(), "amount", 200,
                                "description", "Manure sale"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.source").value("MANUAL"));
        mvc.perform(post("/api/finance/transactions").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("accountUuid", accountUuid, "type", "EXPENSE",
                                "category", "UTILITIES", "date", today.toString(), "amount", 50,
                                "description", "Water and electricity"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/finance/transactions").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("accountUuid", accountUuid, "type", "EXPENSE",
                                "category", "OTHER_INCOME", "date", today.toString(), "amount", 2,
                                "description", "Invalid type/category"))))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/finance/recurring").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Monthly rent", "accountUuid", accountUuid, "type", "EXPENSE",
                                "category", "OTHER_EXPENSE", "frequency", "DAILY", "amount", 10,
                                "startDate", today.toString(), "endDate", today.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(post("/api/finance/recurring/generate").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.generated").value(1));
        mvc.perform(post("/api/finance/recurring/generate").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.generated").value(0));
        mvc.perform(get("/api/finance/accounts").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].balance").value(1140));
        mvc.perform(get("/api/finance/summary").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.income").value(200))
                .andExpect(jsonPath("$.expenses").value(60)).andExpect(jsonPath("$.operatingResult").value(140));
        mvc.perform(get("/api/finance/reports").header("Authorization", auth)
                        .param("from", today.toString()).param("to", today.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));

        JsonNode otherFarm = json(mvc.perform(post("/api/farms").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Other Finance Farm", "ownerName", "Owner"))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(get("/api/finance/accounts").header("Authorization", auth)
                        .header("X-Farm-UUID", otherFarm.get("uuid").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(patch("/api/finance/accounts/{uuid}/status", accountUuid).header("Authorization", auth)
                        .header("X-Farm-UUID", otherFarm.get("uuid").asText())
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("active", false))))
                .andExpect(status().isNotFound());
    }

    private JsonNode json(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }
    private String body(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }
}
