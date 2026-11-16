package com.dairyfarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:dfms-phase-two-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ADMIN_USERNAME=phase-two-admin",
        "ADMIN_PASSWORD=secure-phase-two-password-123",
        "ADMIN_FARM_NAME=Phase Two Farm",
        "app.upload-dir=target/phase-two-test-uploads",
        "app.reproduction.gestation-days=285"
})
class PhaseTwoApiTest {
    private static final String TEST_JWT_SECRET = java.util.Base64.getEncoder()
            .encodeToString(new java.security.SecureRandom().generateSeed(48));

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> TEST_JWT_SECRET);
    }

    @Test
    void managesHealthPregnancyRemindersCalvingAndFarmIsolation() throws Exception {
        String token = login();
        String auth = bearer(token);
        JsonNode breed = json(mvc.perform(get("/api/breeds").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn());
        String breedUuid = breed.get(0).get("uuid").asText();
        String cowUuid = createAnimal(auth, "PH2-COW", "FEMALE", breedUuid);
        createAnimal(auth, "PH2-BULL", "MALE", breedUuid);
        mvc.perform(get("/api/animals/{uuid}", cowUuid).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pregnancyStatus").value("NOT_PREGNANT"));

        Map<String, Object> healthBody = Map.of("animalUuid", cowUuid, "diseaseName", "Mastitis",
                "symptoms", "Swelling", "diagnosis", "Clinical review", "medicineName", "Treatment",
                "treatmentStartDate", LocalDate.now(ZoneId.of("Asia/Kolkata")).toString());
        JsonNode health = json(mvc.perform(post("/api/health/records").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(healthBody)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.animalUuid").value(cowUuid))
                .andReturn());
        String healthUuid = health.get("uuid").asText();
        mvc.perform(put("/api/health/records/{uuid}", healthUuid).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                                "animalUuid", cowUuid, "diseaseName", "Mastitis", "symptoms", "Swelling",
                                "diagnosis", "Updated clinical review", "medicineName", "Treatment",
                                "treatmentStartDate", LocalDate.now(ZoneId.of("Asia/Kolkata")).toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.diagnosis").value("Updated clinical review"));
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream imageBytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", imageBytes);
        MockMultipartFile attachment = new MockMultipartFile("file", "medical.png", "image/png", imageBytes.toByteArray());
        JsonNode uploaded = json(mvc.perform(multipart("/api/health/records/{uuid}/attachments", healthUuid)
                        .file(attachment).header("Authorization", auth))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(get(uploaded.get("downloadUrl").asText()).header("Authorization", auth)).andExpect(status().isOk());

        JsonNode vaccination = json(mvc.perform(post("/api/vaccinations").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("animalUuid", cowUuid, "vaccineName", "Historic vaccine",
                                "vaccinationDate", LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(2).toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.vaccineName").value("Historic vaccine")).andReturn());
        mvc.perform(put("/api/vaccinations/{uuid}", vaccination.get("uuid").asText()).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                                "animalUuid", cowUuid, "vaccineName", "Historic vaccine",
                                "vaccinationDate", LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(2).toString(),
                                "nextVaccinationDate", LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(2).toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nextVaccinationDate").isNotEmpty());

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        String breedingBody = body(Map.of("animalUuid", cowUuid, "breedingDate",
                today.minusDays(1).toString(), "method", "AI", "semenIdentification", "SEMEN-1"));
        JsonNode breeding = json(mvc.perform(post("/api/reproduction/breeding").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(breedingBody))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(get("/api/reminders").header("Authorization", auth).param("bucket", "all"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        JsonNode manualReminder = json(mvc.perform(post("/api/reminders").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                                "animalUuid", cowUuid, "title", "Manual follow-up",
                                "dueAt", today.plusDays(2).atTime(9, 0).toString(), "category", "FOLLOW_UP",
                                "priority", "HIGH"))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/reminders/{uuid}/status", manualReminder.get("uuid").asText())
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("status", "COMPLETED"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        JsonNode notificationPage = json(mvc.perform(get("/api/notifications").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn());
        String notificationUuid = notificationPage.get("content").get(0).get("uuid").asText();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/notifications/{uuid}/read", notificationUuid).header("Authorization", auth))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/reproduction/breeding/{uuid}", breeding.get("uuid").asText())
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(breedingBody))
                .andExpect(status().isOk());
        mvc.perform(get("/api/reminders").header("Authorization", auth).param("bucket", "all"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/reminders").header("Authorization", auth).param("bucket", "completed"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));

        LocalDate examDate = today.minusDays(1);
        json(mvc.perform(post("/api/reproduction/pregnancies").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("animalUuid", cowUuid,
                                "examDate", examDate.toString(), "testType", "ULTRASOUND", "result", "PREGNANT"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.expectedDeliveryDate").value(examDate.plusDays(285).toString()))
                .andReturn());
        mvc.perform(get("/api/animals/{uuid}", cowUuid).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pregnancyStatus").value("PREGNANT"))
                .andExpect(jsonPath("$.expectedDeliveryDate").value(examDate.plusDays(285).toString()));
        mvc.perform(post("/api/reproduction/pregnancies").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("animalUuid", cowUuid, "examDate", today.toString(),
                                "testType", "ULTRASOUND", "result", "NOT_PREGNANT"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/animals/{uuid}", cowUuid).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pregnancyStatus").value("NOT_PREGNANT"))
                .andExpect(jsonPath("$.expectedDeliveryDate").value(org.hamcrest.Matchers.nullValue()));
        JsonNode exam = json(mvc.perform(post("/api/reproduction/pregnancies").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("animalUuid", cowUuid,
                                "examDate", today.toString(), "testType", "ULTRASOUND", "result", "PREGNANT"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.expectedDeliveryDate").value(today.plusDays(285).toString()))
                .andReturn());
        JsonNode heat = json(mvc.perform(post("/api/reproduction/heat").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("animalUuid", cowUuid, "detectionDate", today.minusDays(1).toString(),
                                "symptoms", "Restless and vocal", "observationPeriod", "MORNING"))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(put("/api/reproduction/heat/{uuid}", heat.get("uuid").asText()).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                                "animalUuid", cowUuid, "detectionDate", today.minusDays(1).toString(),
                                "symptoms", "Restless and vocal", "observationPeriod", "EVENING",
                                "observedBy", "Test worker"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.observationPeriod").value("EVENING"));

        JsonNode calving = json(mvc.perform(post("/api/reproduction/calvings").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                                "motherUuid", cowUuid, "actualDeliveryDate", today.toString(),
                                "deliveryType", "NORMAL", "calfCount", 1,
                                "calves", List.of(Map.of("gender", "FEMALE", "birthWeight", 29.5,
                                        "healthStatus", "HEALTHY", "stillborn", false))))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.calves[0].calfUuid").isNotEmpty()).andReturn());
        String calfBirthUuid = calving.get("calves").get(0).get("birthUuid").asText();
        String calfUuid = calving.get("calves").get(0).get("calfUuid").asText();
        mvc.perform(get("/api/animals/{uuid}", calfUuid).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.motherUuid").value(cowUuid));

        mvc.perform(put("/api/reproduction/calvings/{uuid}", calving.get("uuid").asText())
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                                "motherUuid", cowUuid, "actualDeliveryDate", today.toString(),
                                "deliveryType", "ASSISTED", "calfCount", 1,
                                "calves", List.of(Map.of("birthUuid", calfBirthUuid, "gender", "FEMALE",
                                        "birthWeight", 30.0, "healthStatus", "HEALTHY", "stillborn", false))))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/animals").header("Authorization", auth).param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/animals/{uuid}", cowUuid).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pregnancyStatus").value("NOT_PREGNANT"));

        JsonNode unread = json(mvc.perform(get("/api/notifications/unread-count").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn());
        assertTrue(unread.get("unreadCount").asLong() > 0);

        JsonNode secondFarm = json(mvc.perform(post("/api/farms").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("name", "Other Farm", "ownerName", "Owner"))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(post("/api/health/records").header("Authorization", auth)
                        .header("X-Farm-UUID", secondFarm.get("uuid").asText())
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("animalUuid", cowUuid))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/health/records/{uuid}", healthUuid).header("Authorization", auth)
                        .header("X-Farm-UUID", secondFarm.get("uuid").asText()))
                .andExpect(status().isNotFound());

        JsonNode dashboard = json(mvc.perform(get("/api/dashboard").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn());
        assertEquals(1, dashboard.get("calves").asLong());
        assertEquals(1, dashboard.get("vaccinationsDue").asLong());
        assertEquals(1, dashboard.get("medicalTreatmentsInProgress").asLong());
        assertTrue(dashboard.has("overdueReminders"));
        assertEquals(0, dashboard.get("pregnantAnimals").asLong());
        assertEquals(1, dashboard.get("animalsInHeat").asLong());
        assertTrue(exam.has("expectedDeliveryDate"));
    }

    private String login() throws Exception {
        JsonNode result = json(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"phase-two-admin\",\"password\":\"secure-phase-two-password-123\"}"))
                .andExpect(status().isOk()).andReturn());
        return result.get("token").asText();
    }

    private String createAnimal(String auth, String tag, String gender, String breedUuid) throws Exception {
        JsonNode animal = json(mvc.perform(post("/api/animals").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                                "tagNumber", tag, "type", "COW", "gender", gender, "breedUuid", breedUuid))))
                .andExpect(status().isOk()).andReturn());
        return animal.get("uuid").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }
    private String body(Object value) throws Exception { return mapper.writeValueAsString(value); }
    private static String bearer(String token) { return "Bearer " + token; }
}
