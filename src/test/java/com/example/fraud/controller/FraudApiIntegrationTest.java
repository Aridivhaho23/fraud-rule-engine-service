package com.example.fraud.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * INTEGRATION tests: the real application (security, controllers, services, rules, Hibernate, Flyway and an
 * in-memory H2 database) driven through HTTP-like requests. They prove the layers work together, that the
 * Flyway SQL matches the entities, and that role-based access control is enforced.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FraudApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    // ---- helpers: users with different roles, unique test data ----
    private static RequestPostProcessor admin() { return user("admin").roles("ADMIN"); }
    private static RequestPostProcessor analyst() { return user("analyst").roles("ANALYST"); }
    private static RequestPostProcessor ingestion() { return user("ingestion").roles("INGESTION"); }

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private long createCustomer() throws Exception {
        String body = mockMvc.perform(post("/api/v1/customers").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Test Customer","email":"c%s@example.com","homeCountry":"ZA"}
                                """.formatted(unique())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asLong();
    }

    private String transactionJson(long customerId, String reference, String amount, String merchant,
                                   String category, String country, OffsetDateTime at) {
        return """
                {"reference":"%s","customerId":%d,"amount":%s,"merchantName":"%s","merchantCategory":"%s",
                 "country":"%s","occurredAt":"%s"}
                """.formatted(reference, customerId, amount, merchant, category, country, at);
    }

    /** Submits a transaction as the ingestion service, expecting 201, and returns the JSON result. */
    private JsonNode submit(long customerId, String amount, String merchant, String category, String country,
                            OffsetDateTime at) throws Exception {
        String body = mockMvc.perform(post("/api/v1/transactions").with(ingestion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson(customerId, "TX-" + unique(), amount, merchant, category, country, at)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    /** One minute ago, in UTC, at daytime-neutral precision. */
    private static OffsetDateTime recently() {
        return OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1);
    }

    private static boolean hasRule(JsonNode result, String ruleCode) {
        for (JsonNode flag : result.get("flags")) {
            if (ruleCode.equals(flag.get("ruleCode").asText())) {
                return true;
            }
        }
        return false;
    }

    // ---- customer administration ----

    @Test
    void adminCanReplaceCustomerProfile_andDeleteUnusedCustomer() throws Exception {
        long customerId = createCustomer();

        mockMvc.perform(put("/api/v1/customers/" + customerId).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated Customer\",\"email\":\"updated-" + unique() + "@example.com\",\"homeCountry\":\"US\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.fullName").value("Updated Customer"))
                .andExpect(jsonPath("$.homeCountry").value("US"));

        mockMvc.perform(delete("/api/v1/customers/" + customerId).with(admin()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/customers/" + customerId).with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerWithTransactionsCannotBeDeleted() throws Exception {
        long customerId = createCustomer();
        submit(customerId, "100.00", "Corner Shop", "GROCERIES", "ZA", recently());

        mockMvc.perform(delete("/api/v1/customers/" + customerId).with(admin()))
                .andExpect(status().isConflict());
    }

    @Test
    void analystCannotUpdateOrDeleteCustomer() throws Exception {
        long customerId = createCustomer();
        mockMvc.perform(put("/api/v1/customers/" + customerId).with(analyst())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Not allowed\",\"email\":\"blocked-" + unique() + "@example.com\",\"homeCountry\":\"ZA\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/customers/" + customerId).with(analyst()))
                .andExpect(status().isForbidden());
    }

    // ---- fraud screening ----

    @Test
    void ordinaryTransaction_isNotFlagged() throws Exception {
        long customer = createCustomer();

        JsonNode result = submit(customer, "120.50", "Corner Shop", "GROCERIES", "ZA", recently());

        // (a night-time run of the test could add NIGHT_TRANSACTION only for amounts >= R500, so R120 is always clean)
        org.junit.jupiter.api.Assertions.assertFalse(result.get("flagged").asBoolean());
    }

    @Test
    void veryLargeTransaction_isFlaggedCritical() throws Exception {
        long customer = createCustomer();

        JsonNode result = submit(customer, "60000.00", "Gadget World", "RETAIL", "ZA", recently());

        org.junit.jupiter.api.Assertions.assertTrue(hasRule(result, "HIGH_AMOUNT"));
        org.junit.jupiter.api.Assertions.assertEquals("CRITICAL", result.get("highestSeverity").asText());
    }

    @Test
    void sixTransactionsQuickly_triggerVelocityAndCardTesting() throws Exception {
        long customer = createCustomer();
        OffsetDateTime at = recently();

        JsonNode last = null;
        for (int i = 0; i < 6; i++) {
            last = submit(customer, "10.00", "Web Shop", "OTHER", "ZA", at);
        }

        org.junit.jupiter.api.Assertions.assertTrue(hasRule(last, "VELOCITY"));
        org.junit.jupiter.api.Assertions.assertTrue(hasRule(last, "CARD_TESTING"));
    }

    @Test
    void purchasesInTwoCountriesWithinAnHour_triggerGeographyConflict() throws Exception {
        long customer = createCustomer();
        submit(customer, "300.00", "Shop ZA", "RETAIL", "ZA", recently());

        JsonNode second = submit(customer, "300.00", "Shop US", "RETAIL", "US", recently());

        org.junit.jupiter.api.Assertions.assertTrue(hasRule(second, "GEO_CONFLICT"));
    }

    @Test
    void blacklistedMerchant_isFlaggedCritical_untilRemoved() throws Exception {
        long customer = createCustomer();
        String merchant = "Evil Corp " + unique();

        String added = mockMvc.perform(post("/api/v1/blacklist").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantName\":\"" + merchant + "\",\"reason\":\"test\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long blacklistId = mapper.readTree(added).get("id").asLong();

        mockMvc.perform(put("/api/v1/blacklist/" + blacklistId).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantName\":\"" + merchant + "\",\"reason\":\"updated test reason\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("updated test reason"));

        org.junit.jupiter.api.Assertions.assertTrue(
                hasRule(submit(customer, "100.00", merchant.toUpperCase(), "OTHER", "ZA", recently()), "BLACKLIST_MERCHANT"));

        mockMvc.perform(delete("/api/v1/blacklist/" + blacklistId).with(admin())).andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(
                hasRule(submit(customer, "100.00", merchant, "OTHER", "ZA", recently()), "BLACKLIST_MERCHANT"));
    }

    @Test
    void ingestionCanSubmitAnAtomicBatch() throws Exception {
        long customer = createCustomer();
        String first = transactionJson(customer, "BATCH-A-" + unique(), "100.00", "Corner Shop",
                "GROCERIES", "ZA", recently()).trim();
        String second = transactionJson(customer, "BATCH-B-" + unique(), "60000.00", "Gadget World",
                "RETAIL", "ZA", recently()).trim();
        String payload = "{\"transactions\":[" + first + "," + second + "]}";

        mockMvc.perform(post("/api/v1/transactions/batch").with(ingestion())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.received").value(2))
                .andExpect(jsonPath("$.results.length()").value(2));
    }

    @Test
    void batchWithDuplicateReferences_isRejected() throws Exception {
        long customer = createCustomer();
        String reference = "BATCH-DUP-" + unique();
        String first = transactionJson(customer, reference, "25.00", "Corner Shop",
                "GROCERIES", "ZA", recently()).trim();
        String second = transactionJson(customer, reference, "35.00", "Another Shop",
                "GROCERIES", "ZA", recently()).trim();
        String payload = "{\"transactions\":[" + first + "," + second + "]}";

        mockMvc.perform(post("/api/v1/transactions/batch").with(ingestion())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void blacklistHistoryIsAdminOnly() throws Exception {
        mockMvc.perform(get("/api/v1/blacklist/history").with(analyst()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/blacklist/history").with(admin()))
                .andExpect(status().isOk());
    }

    // ---- analyst workflow ----

    @Test
    void analystCanSearchFlags_reviewThem_andSeeTheAuditTrail() throws Exception {
        long customer = createCustomer();
        JsonNode result = submit(customer, "60000.00", "Gadget World", "RETAIL", "ZA", recently());
        long flagId = result.get("flags").get(0).get("id").asLong();

        mockMvc.perform(get("/api/v1/flags?customerId=" + customer + "&severity=CRITICAL").with(analyst()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));

        mockMvc.perform(patch("/api/v1/flags/" + flagId + "/status").with(analyst())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNDER_REVIEW\",\"note\":\"calling the customer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.reviewedBy").value("analyst"));

        mockMvc.perform(patch("/api/v1/flags/" + flagId + "/status").with(analyst())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FALSE_POSITIVE\",\"note\":\"customer confirmed purchase\"}"))
                .andExpect(status().isOk());

        // a final status cannot be changed any more -> 422
        mockMvc.perform(patch("/api/v1/flags/" + flagId + "/status").with(analyst())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/v1/flags/" + flagId + "/history").with(analyst()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].changedBy").value("analyst"))
                .andExpect(jsonPath("$[1].toStatus").value("FALSE_POSITIVE"));
    }

    // ---- validation, idempotency, errors ----

    @Test
    void duplicateReference_returns409() throws Exception {
        long customer = createCustomer();
        String json = transactionJson(customer, "DUP-" + unique(), "100.00", "Shop", "RETAIL", "ZA", recently());

        mockMvc.perform(post("/api/v1/transactions").with(ingestion()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/transactions").with(ingestion()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void analystCanFindTransactionByStableReference() throws Exception {
        long customer = createCustomer();
        String reference = "LOOKUP-" + unique();
        mockMvc.perform(post("/api/v1/transactions").with(ingestion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson(customer, reference, "125.00", "Corner Shop", "GROCERIES", "ZA", recently())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/transactions/reference/" + reference).with(analyst()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.reference").value(reference))
                .andExpect(jsonPath("$.transaction.customerId").value(customer));
    }

    @Test
    void invalidTransaction_returns400_withFieldViolations() throws Exception {
        mockMvc.perform(post("/api/v1/transactions").with(ingestion()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reference\":\"\",\"amount\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations").isNotEmpty());
    }

    @Test
    void unknownCustomer_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/transactions").with(ingestion()).contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson(999999L, "TX-" + unique(), "10.00", "Shop", "RETAIL", "ZA", recently())))
                .andExpect(status().isNotFound());
    }

    @Test
    void futureTimestamp_returns422() throws Exception {
        long customer = createCustomer();

        mockMvc.perform(post("/api/v1/transactions").with(ingestion()).contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson(customer, "TX-" + unique(), "10.00", "Shop", "RETAIL", "ZA",
                                OffsetDateTime.now(ZoneOffset.UTC).plusDays(1))))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---- security: least privilege ----

    @Test
    void requestsWithoutCredentials_return401() throws Exception {
        mockMvc.perform(get("/api/v1/flags")).andExpect(status().isUnauthorized());
    }

    @Test
    void ingestionServiceCannotReadFlags_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/flags").with(ingestion())).andExpect(status().isForbidden());
    }

    @Test
    void analystCannotSubmitTransactionsOrManageTheBlacklist_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/transactions").with(analyst()).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/blacklist").with(analyst()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantName\":\"x\",\"reason\":\"y\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
