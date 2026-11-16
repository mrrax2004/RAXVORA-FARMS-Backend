package com.dairyfarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dairyfarm.service.MilkManagementService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.List;
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
        "spring.datasource.url=jdbc:h2:mem:dfms-phase-three-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ADMIN_USERNAME=phase-three-admin",
        "ADMIN_PASSWORD=secure-phase-three-password-123",
        "ADMIN_FARM_NAME=Phase Three Farm",
        "app.upload-dir=target/phase-three-test-uploads"
})
class PhaseThreeApiTest {
    private static final String TEST_JWT_SECRET = java.util.Base64.getEncoder()
            .encodeToString(new java.security.SecureRandom().generateSeed(48));
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private MilkManagementService milk;
    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry registry) { registry.add("JWT_SECRET", () -> TEST_JWT_SECRET); }

    @Test
    void managesProductionStockSalesPaymentsSubscriptionsReportsAndFarmIsolation() throws Exception {
        String auth = "Bearer " + json(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"phase-three-admin\",\"password\":\"secure-phase-three-password-123\"}"))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
        mvc.perform(put("/api/milk/default-rate").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("ratePerLitre",10))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ratePerLitre").value(10));
        String breedUuid = json(mvc.perform(get("/api/breeds").header("Authorization",auth))
                .andExpect(status().isOk()).andReturn()).get(0).get("uuid").asText();
        JsonNode animal = json(mvc.perform(post("/api/animals").header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("tagNumber","MILK-COW-1",
                                "type","COW","gender","FEMALE","breedUuid",breedUuid))))
                .andExpect(status().isOk()).andReturn());
        String animalUuid=animal.get("uuid").asText();
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Kolkata"));
        mvc.perform(post("/api/milk/production").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("animalUuid",animalUuid,"productionDate",today.toString(),"session","EVENING","quantity",3))))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/milk/animals/{uuid}/lactation",animalUuid).header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("lactating",true))))
                .andExpect(status().isOk());

        JsonNode production=json(mvc.perform(post("/api/milk/production").header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("animalUuid",animalUuid,
                                "productionDate",today.toString(),"session","MORNING","quantity",8.5))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(8.5)).andReturn());
        mvc.perform(post("/api/milk/production").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("animalUuid",animalUuid,"productionDate",today.toString(),"session","MORNING","quantity",9))))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/milk/production/{uuid}",production.get("uuid").asText()).header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("quantity",8.75,"correctionReason","Scale correction"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(8.75));
        mvc.perform(get("/api/milk/production/{uuid}/audit",production.get("uuid").asText()).header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].correctionReason").value("Scale correction"));

        JsonNode customer=json(mvc.perform(post("/api/milk/customers").header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("name","Corner Shop","mobileNumber","123",
                                "type","SHOP","startDate",today.toString(),"status","ACTIVE"))))
                .andExpect(status().isOk()).andReturn());
        String customerUuid=customer.get("uuid").asText();
        mvc.perform(get("/api/milk/customers/{uuid}/rates/applicable",customerUuid).header("Authorization",auth)
                        .param("date",today.minusDays(1).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ratePerLitre").value(10))
                .andExpect(jsonPath("$.source").value("FARM_DEFAULT"));
        JsonNode fixedRate=json(mvc.perform(post("/api/milk/customers/{uuid}/rates",customerUuid).header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("ratePerLitre",48,"effectiveFrom",today.toString(),
                                "effectiveUntil",today.toString(),"status","ACTIVE","notes","Contract price"))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(get("/api/milk/customers/{uuid}/rates/applicable",customerUuid).header("Authorization",auth)
                        .param("date",today.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ratePerLitre").value(48))
                .andExpect(jsonPath("$.source").value("CUSTOMER_FIXED"));
        mvc.perform(post("/api/milk/customers/{uuid}/rates",customerUuid).header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("ratePerLitre",49,"effectiveFrom",today.toString(),
                                "status","ACTIVE"))))
                .andExpect(status().isConflict());
        JsonNode scheduledRate=json(mvc.perform(post("/api/milk/customers/{uuid}/rates",customerUuid).header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("ratePerLitre",52,"effectiveFrom",today.plusDays(1).toString(),
                                "status","ACTIVE","notes","Scheduled rate"))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(put("/api/milk/customers/{uuid}/rates/{rateUuid}",customerUuid,scheduledRate.get("uuid").asText())
                        .header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("ratePerLitre",53,"effectiveFrom",today.plusDays(1).toString(),
                                "status","ACTIVE","notes","Scheduled rate updated"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ratePerLitre").value(53));
        mvc.perform(get("/api/milk/customers/{uuid}/rates",customerUuid).header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/milk/stock").header("Authorization",auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(8.75));
        mvc.perform(post("/api/milk/stock/opening").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("quantity",11.25,"note","Initial stock"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.availableQuantity").value(20));
        mvc.perform(post("/api/milk/stock/opening").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("quantity",1,"note","Duplicate opening"))))
                .andExpect(status().isConflict());

        JsonNode sale=json(mvc.perform(post("/api/milk/sales").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("invoiceNumber","INV-001","customerUuid",customerUuid,"saleDate",today.toString(),
                                "quantity",5,"unitPrice",10,"discount",0,"paymentMethod","UPI","initialPayment",20))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(240))
                .andExpect(jsonPath("$.unitPrice").value(48)).andExpect(jsonPath("$.originalUnitPrice").value(48))
                .andExpect(jsonPath("$.paidAmount").value(20)).andExpect(jsonPath("$.paymentStatus").value("PARTIAL")).andReturn());
        mvc.perform(get("/api/milk/customers/{uuid}",customerUuid).header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outstandingBalance").value(220));
        mvc.perform(post("/api/milk/payments").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("customerUuid",customerUuid,"saleUuid",sale.get("uuid").asText(),
                                "paymentDate",today.toString(),"amount",221,"method","CASH"))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/milk/payments").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("customerUuid",customerUuid,"saleUuid",sale.get("uuid").asText(),
                                "paymentDate",today.toString(),"amount",220,"method","CASH"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/milk/customers/{uuid}",customerUuid).header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outstandingBalance").value(0));
        mvc.perform(get("/api/milk/sales/{uuid}",sale.get("uuid").asText()).header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paymentStatus").value("PAID"));
        mvc.perform(post("/api/milk/sales").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("invoiceNumber","INV-002","customerUuid",customerUuid,"saleDate",today.toString(),
                                "quantity",100,"unitPrice",10,"discount",0,"paymentMethod","CREDIT"))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/milk/sales").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("invoiceNumber","INV-OVR","customerUuid",customerUuid,"saleDate",today.toString(),
                                "quantity",1,"discount",0,"paymentMethod","CASH","overrideRate",50,
                                "overrideReason","Approved contract exception"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unitPrice").value(50))
                .andExpect(jsonPath("$.originalUnitPrice").value(48))
                .andExpect(jsonPath("$.rateOverrideReason").value("Approved contract exception"));
        JsonNode defaultCustomer=json(mvc.perform(post("/api/milk/customers").header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("name","Walk-in Customer","type","INDIVIDUAL",
                                "startDate",today.toString(),"status","ACTIVE"))))
                .andExpect(status().isOk()).andReturn());
        String defaultCustomerUuid=defaultCustomer.get("uuid").asText();
        mvc.perform(post("/api/milk/sales").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("invoiceNumber","INV-DEFAULT","customerUuid",defaultCustomerUuid,"saleDate",today.toString(),
                                "quantity",1,"unitPrice",99,"discount",0,"paymentMethod","CREDIT"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unitPrice").value(10))
                .andExpect(jsonPath("$.totalAmount").value(10));

        JsonNode subscription=json(mvc.perform(post("/api/milk/subscriptions").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("customerUuid",customerUuid,"dailyQuantity",2,"session","EVENING",
                                "startDate",today.toString(),"unitPrice",9))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unitPrice").value(48)).andReturn());
        mvc.perform(put("/api/milk/subscriptions/{uuid}",subscription.get("uuid").asText()).header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("customerUuid",customerUuid,
                                "dailyQuantity",2,"session","EVENING","startDate",today.toString(),"unitPrice",9))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unitPrice").value(48));
        mvc.perform(post("/api/milk/deliveries/generate").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("date",today.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.generated").value(1));
        mvc.perform(post("/api/milk/deliveries/generate").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("date",today.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.generated").value(0));
        JsonNode delivery=json(mvc.perform(get("/api/milk/deliveries").header("Authorization",auth)
                        .param("from",today.toString()).param("to",today.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].unitPrice").value(48))
                .andExpect(jsonPath("$.content[0].totalAmount").value(0)).andReturn()).get("content").get(0);
        mvc.perform(patch("/api/milk/deliveries/{uuid}",delivery.get("uuid").asText()).header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status","PARTIAL","deliveredQuantity",1))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.totalAmount").value(48));
        mvc.perform(patch("/api/milk/deliveries/{uuid}",delivery.get("uuid").asText()).header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status","DELIVERED","deliveredQuantity",2))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(96));
        mvc.perform(get("/api/milk/stock").header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.availableQuantity").value(11));

        mvc.perform(get("/api/milk/reports.csv").header("Authorization",auth).param("type","sales")
                        .param("from",today.toString()).param("to",today.toString()))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("INV-001")));
        mvc.perform(get("/api/milk/dashboard").header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activeCustomers").value(2))
                .andExpect(jsonPath("$.todayRevenue").value(300)).andExpect(jsonPath("$.availableStock").value(11));

        JsonNode adminUser=json(mvc.perform(get("/api/auth/me").header("Authorization",auth))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(post("/api/users").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username","milk-rate-manager","password","secure-manager-password",
                                "fullName","Milk Rate Manager","farmUuid",adminUser.get("farmUuid").asText(),"roles",List.of("MANAGER")))))
                .andExpect(status().isOk());
        String managerAuth="Bearer "+json(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"milk-rate-manager\",\"password\":\"secure-manager-password\"}"))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
        mvc.perform(post("/api/milk/customers/{uuid}/rates",defaultCustomerUuid).header("Authorization",managerAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("ratePerLitre",55,
                                "effectiveFrom",today.plusDays(2).toString(),"status","ACTIVE","notes","Manager configured"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ratePerLitre").value(55));
        mvc.perform(put("/api/milk/default-rate").header("Authorization",managerAuth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("ratePerLitre",11))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ratePerLitre").value(11));
        mvc.perform(post("/api/users").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username","milk-rate-worker","password","secure-worker-password",
                                "fullName","Milk Rate Worker","farmUuid",adminUser.get("farmUuid").asText(),"roles",List.of("WORKER")))))
                .andExpect(status().isOk());
        String workerAuth="Bearer "+json(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"milk-rate-worker\",\"password\":\"secure-worker-password\"}"))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
        mvc.perform(post("/api/milk/sales").header("Authorization",workerAuth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("invoiceNumber","INV-WORKER","customerUuid",customerUuid,"saleDate",today.toString(),
                                "quantity",1,"discount",0,"paymentMethod","CASH","overrideRate",99,"overrideReason","Test"))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/milk/sales").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("invoiceNumber","INV-NO-REASON","customerUuid",customerUuid,"saleDate",today.toString(),
                                "quantity",1,"discount",0,"paymentMethod","CASH","overrideRate",50))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/milk/customers/{uuid}/rates",customerUuid).header("Authorization",workerAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("ratePerLitre",99,"effectiveFrom",today.toString(),
                                "status","ACTIVE"))))
                .andExpect(status().isForbidden());

        assertEquals(1,milk.generateForFarm(java.util.UUID.fromString(adminUser.get("farmUuid").asText()),today.plusDays(1)));
        mvc.perform(get("/api/milk/deliveries").header("Authorization",auth)
                        .param("from",today.plusDays(1).toString()).param("to",today.plusDays(1).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].unitPrice").value(53))
                .andExpect(jsonPath("$.content[0].totalAmount").value(0));
        String futureDelivery=json(mvc.perform(get("/api/milk/deliveries").header("Authorization",auth)
                        .param("from",today.plusDays(1).toString()).param("to",today.plusDays(1).toString()))
                .andExpect(status().isOk()).andReturn()).get("content").get(0).get("uuid").asText();
        mvc.perform(patch("/api/milk/deliveries/{uuid}",futureDelivery).header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status","DELIVERED","deliveredQuantity",2))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(106));
        mvc.perform(patch("/api/milk/customers/{uuid}/rates/{rateUuid}/status",customerUuid,scheduledRate.get("uuid").asText())
                        .header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("status","INACTIVE"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        mvc.perform(get("/api/milk/customers/{uuid}/rates/applicable",customerUuid).header("Authorization",auth)
                        .param("date",today.plusDays(1).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ratePerLitre").value(11))
                .andExpect(jsonPath("$.source").value("FARM_DEFAULT"));
        mvc.perform(get("/api/milk/sales/{uuid}",sale.get("uuid").asText()).header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unitPrice").value(48))
                .andExpect(jsonPath("$.totalAmount").value(240));
        mvc.perform(get("/api/milk/deliveries").header("Authorization",auth)
                        .param("from",today.toString()).param("to",today.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].unitPrice").value(48))
                .andExpect(jsonPath("$.content[0].totalAmount").value(96));

        JsonNode otherFarm=json(mvc.perform(post("/api/farms").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name","Isolated Farm","ownerName","Owner"))))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(get("/api/milk/customers/{uuid}",customerUuid).header("Authorization",auth)
                        .header("X-Farm-UUID",otherFarm.get("uuid").asText()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/milk/customers/{uuid}/rates",customerUuid).header("Authorization",auth)
                        .header("X-Farm-UUID",otherFarm.get("uuid").asText()))
                .andExpect(status().isNotFound());
        JsonNode emptyStock=json(mvc.perform(get("/api/milk/stock").header("Authorization",auth)
                        .header("X-Farm-UUID",otherFarm.get("uuid").asText()))
                .andExpect(status().isOk()).andReturn());
        assertEquals(0,emptyStock.get("availableQuantity").asInt());
    }

    private JsonNode json(MvcResult result)throws Exception{return mapper.readTree(result.getResponse().getContentAsString());}
    private String body(Object value)throws Exception{return mapper.writeValueAsString(value);}
    private LocalDate today(){return LocalDate.now(ZoneId.of("Asia/Kolkata"));}
}
