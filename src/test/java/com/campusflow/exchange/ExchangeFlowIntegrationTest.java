package com.campusflow.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusflow.support.IntegrationTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** The full marketplace flow over HTTP against MySQL: listing, request, deal, review, notifications. */
class ExchangeFlowIntegrationTest extends IntegrationTest {

    private long createItem(TestUser owner, String title, String type) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of(
                "categoryId", 2, "title", title, "description", "Còn tốt", "itemType", type, "condition", "USED"));
        if ("SALE".equals(type)) {
            body.put("price", 50000);
        }
        return read(mockMvc.perform(as(owner, withJson(post("/api/items"), body))).andExpect(status().isCreated()))
                .path("id").asLong();
    }

    private long request(TestUser requester, long itemId) throws Exception {
        return read(mockMvc.perform(as(requester, withJson(post("/api/exchange-requests"), Map.of("itemId", itemId))))
                .andExpect(status().isCreated())).path("id").asLong();
    }

    @Test
    void giveawayFromRequestToReview() throws Exception {
        TestUser owner = registerVerifiedUser("Owner");
        TestUser requester = registerVerifiedUser("Requester");
        long itemId = createItem(owner, "Nồi cơm điện", "GIVEAWAY");

        long requestId = request(requester, itemId);
        mockMvc.perform(as(owner, get("/api/notifications/unread-count")))
                .andExpect(jsonPath("$.unread").value(1));
        assertThat(mail.awaitEmail(owner.email(), "Yêu cầu mới").getText()).contains("Requester muốn nhận");

        mockMvc.perform(as(owner, post("/api/exchange-requests/{id}/accept", requestId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counterpartContact.email").value(requester.email()));
        mockMvc.perform(get("/api/items/{id}", itemId)).andExpect(jsonPath("$.status").value("IN_TRANSACTION"));

        mockMvc.perform(as(owner, post("/api/exchange-requests/{id}/complete", requestId)))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(as(requester, withJson(post("/api/exchange-requests/{id}/reviews", requestId),
                        Map.of("rating", 4, "comment", "Thân thiện"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/{id}", owner.id()))
                .andExpect(jsonPath("$.reviewCount").value(1))
                .andExpect(jsonPath("$.reputationScore").value(4.0))
                .andExpect(jsonPath("$.email").doesNotExist());
        JsonNode requesterNotifications = read(mockMvc.perform(as(requester, get("/api/notifications"))));
        assertThat(requesterNotifications.path("content").findValuesAsString("type"))
                .contains("EXCHANGE_ACCEPTED", "EXCHANGE_COMPLETED");
    }

    /** Repeated because race conditions do not show up on every run. */
    @RepeatedTest(5)
    void simultaneousAcceptsLetExactlyOneWin() throws Exception {
        TestUser owner = registerVerifiedUser("Owner");
        long itemId = createItem(owner, "Quạt mini", "GIVEAWAY");
        List<Long> requestIds = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            requestIds.add(request(registerVerifiedUser("Req" + i), itemId));
        }

        List<Callable<Integer>> accepts = requestIds.stream()
                .<Callable<Integer>>map(id -> () -> mockMvc
                        .perform(as(owner, post("/api/exchange-requests/{id}/accept", id)))
                        .andReturn().getResponse().getStatus())
                .toList();
        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(accepts.size())) {
            for (Future<Integer> result : pool.invokeAll(accepts)) {
                statuses.add(result.get());
            }
        }

        assertThat(statuses).containsOnlyOnce(200).containsOnly(200, 409);
        JsonNode received = read(mockMvc.perform(as(owner, get("/api/exchange-requests/received"))));
        assertThat(received.path("content").findValuesAsString("status"))
                .containsOnlyOnce("ACCEPTED")
                .doesNotContain("PENDING");
    }
}
