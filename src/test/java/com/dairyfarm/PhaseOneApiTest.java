package com.dairyfarm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:dfms-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ADMIN_USERNAME=test-admin",
        "ADMIN_PASSWORD=secure-test-password-123",
        "ADMIN_FARM_NAME=Test Farm",
        "app.upload-dir=target/test-uploads"
})
class PhaseOneApiTest {
    private static final String TEST_JWT_SECRET = java.util.Base64.getEncoder()
            .encodeToString(new java.security.SecureRandom().generateSeed(48));
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> TEST_JWT_SECRET);
    }

    @Test
    void authenticatesManagesAnimalsAndEnforcesFarmIsolation() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test-admin\",\"password\":\"secure-test-password-123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty()).andReturn();
        String adminToken = mapper.readTree(login.getResponse().getContentAsString()).get("token").asText();

        JsonNode breedList = mapper.readTree(mvc.perform(get("/api/breeds").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(8, breedList.size());
        String breedUuid = breedList.get(0).get("uuid").asText();
        String animalResponse = mvc.perform(post("/api/animals").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagNumber\":\"TEST-001\",\"name\":\"Daisy\",\"type\":\"COW\",\"gender\":\"FEMALE\",\"breedUuid\":\""
                                + breedUuid + "\",\"purchasePrice\":1250.50}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tagNumber").value("TEST-001"))
                .andExpect(jsonPath("$.purchasePrice").value(1250.50)).andReturn().getResponse().getContentAsString();
        String animalUuid = mapper.readTree(animalResponse).get("uuid").asText();

        String secondFarm = mvc.perform(post("/api/farms").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Second Farm\",\"ownerName\":\"Second Owner\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String secondFarmUuid = mapper.readTree(secondFarm).get("uuid").asText();
        mvc.perform(get("/api/breeds").header("Authorization", bearer(adminToken)).header("X-Farm-UUID", secondFarmUuid))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(8));
        mvc.perform(get("/api/animals/{uuid}", animalUuid).header("Authorization", bearer(adminToken))
                        .header("X-Farm-UUID", secondFarmUuid))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/users").header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"second-manager\",\"password\":\"secure-manager-password\",\"fullName\":\"Manager Two\","
                                + "\"farmUuid\":\"" + secondFarmUuid + "\",\"roles\":[\"MANAGER\"]}"))
                .andExpect(status().isOk());

        String managerLogin = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"second-manager\",\"password\":\"secure-manager-password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String managerToken = mapper.readTree(managerLogin).get("token").asText();
        mvc.perform(get("/api/animals/{uuid}", animalUuid).header("Authorization", bearer(managerToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/dashboard").header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAnimals").value(0));

        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream imageBytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", imageBytes);
        MockMultipartFile photo = new MockMultipartFile("file", "animal.png", "image/png", imageBytes.toByteArray());
        String uploadResponse = mvc.perform(multipart("/api/animals/photos").file(photo)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String photoUrl = mapper.readTree(uploadResponse).get("url").asText();
        mvc.perform(get(photoUrl).header("Authorization", bearer(adminToken))).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").header("Authorization", bearer(adminToken))).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(adminToken))).andExpect(status().isUnauthorized());
    }

    private static String bearer(String token) { return "Bearer " + token; }
}
