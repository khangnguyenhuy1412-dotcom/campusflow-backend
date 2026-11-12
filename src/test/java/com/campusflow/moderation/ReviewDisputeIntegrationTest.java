package com.campusflow.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusflow.support.IntegrationTest;
import com.campusflow.user.entity.Role;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** A disputed 1-star review: reply, report, clarification via support tickets, hiding, reputation. */
class ReviewDisputeIntegrationTest extends IntegrationTest {

    private record Deal(TestUser seller, TestUser buyer, long requestId) {
    }

    private Deal completedDeal() throws Exception {
        TestUser seller = registerVerifiedUser("Seller");
        TestUser buyer = registerVerifiedUser("Buyer");
        long itemId = read(mockMvc.perform(as(seller, withJson(post("/api/items"), Map.of(
                "categoryId", 2, "title", "Nồi cơm điện", "description", "Còn tốt",
                "itemType", "GIVEAWAY", "condition", "USED"))))).path("id").asLong();
        long requestId = read(mockMvc.perform(as(buyer, withJson(post("/api/exchange-requests"),
                Map.of("itemId", itemId))))).path("id").asLong();
        mockMvc.perform(as(seller, post("/api/exchange-requests/{id}/accept", requestId))).andExpect(status().isOk());
        mockMvc.perform(as(seller, post("/api/exchange-requests/{id}/complete", requestId))).andExpect(status().isOk());
        return new Deal(seller, buyer, requestId);
    }

    private long review(TestUser reviewer, long requestId, int rating, String comment) throws Exception {
        return read(mockMvc.perform(as(reviewer, withJson(post("/api/exchange-requests/{id}/reviews", requestId),
                Map.of("rating", rating, "comment", comment)))).andExpect(status().isCreated())).path("id").asLong();
    }

    @Test
    void reviewedPersonRepliesOnce() throws Exception {
        Deal deal = completedDeal();
        long reviewId = review(deal.buyer(), deal.requestId(), 1, "Nồi hỏng, lừa đảo");

        mockMvc.perform(as(deal.buyer(), withJson(put("/api/reviews/{id}/reply", reviewId), Map.of("reply", "x"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(deal.seller(), withJson(put("/api/reviews/{id}/reply", reviewId),
                        Map.of("reply", "Nồi chạy tốt khi giao, bạn đã kiểm tra tại chỗ."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Nồi chạy tốt khi giao, bạn đã kiểm tra tại chỗ."));
        mockMvc.perform(as(deal.seller(), withJson(put("/api/reviews/{id}/reply", reviewId), Map.of("reply", "Sửa"))))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/users/{id}/reviews", deal.seller().id()))
                .andExpect(jsonPath("$.content[0].reply").value("Nồi chạy tốt khi giao, bạn đã kiểm tra tại chỗ."))
                .andExpect(jsonPath("$.content[0].repliedAt").isNotEmpty());
        assertThat(notificationTypes(deal.buyer())).contains("REVIEW_REPLIED");
    }

    @Test
    void falseReviewIsReportedClarifiedWithBothSidesAndHidden() throws Exception {
        Deal deal = completedDeal();
        long reviewId = review(deal.buyer(), deal.requestId(), 1, "Lừa đảo");
        mockMvc.perform(get("/api/users/{id}", deal.seller().id())).andExpect(jsonPath("$.reputationScore").value(1.0));
        TestUser moderator = withRole(registerVerifiedUser("Mod"), Role.MODERATOR);

        // The seller reports the review; the reviewer cannot report their own review
        long reportId = read(mockMvc.perform(as(deal.seller(), withJson(post("/api/reports"),
                        Map.of("targetType", "REVIEW", "targetId", reviewId, "reason", "OTHER",
                                "details", "Đánh giá sai sự thật, có ảnh lúc giao nhận"))))
                .andExpect(status().isCreated())).path("id").asLong();
        mockMvc.perform(as(deal.seller(), withJson(post("/api/reports"),
                Map.of("targetType", "REVIEW", "targetId", reviewId, "reason", "OTHER")))).andExpect(status().isConflict());
        mockMvc.perform(as(deal.buyer(), withJson(post("/api/reports"),
                Map.of("targetType", "REVIEW", "targetId", reviewId, "reason", "OTHER")))).andExpect(status().isBadRequest());
        mockMvc.perform(as(moderator, get("/api/moderation/reports/{id}", reportId)))
                .andExpect(jsonPath("$.targetReviewId").value(reviewId))
                .andExpect(jsonPath("$.targetReviewRating").value(1))
                .andExpect(jsonPath("$.targetUser.id").value(deal.buyer().id()));

        // The moderator asks the reviewer for their side
        long ticketId = read(mockMvc.perform(as(moderator, withJson(post("/api/moderation/support-tickets"), Map.of(
                        "userId", deal.buyer().id(), "subject", "Làm rõ đánh giá 1 sao",
                        "message", "Bạn có thể gửi ảnh nồi bị hỏng không?", "relatedReportId", reportId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.openedByStaff").value(true))).path("id").asLong();
        assertThat(notificationTypes(deal.buyer())).contains("SUPPORT_REPLY");
        assertThat(mail.awaitEmail(deal.buyer().email(), "Ban quản trị đã trả lời").getText()).contains("gửi ảnh");

        // The user sees the team, not the moderator's identity
        mockMvc.perform(as(deal.buyer(), get("/api/support/tickets/{id}/messages", ticketId)))
                .andExpect(jsonPath("$[0].senderName").value("Ban quản trị CampusFlow"))
                .andExpect(jsonPath("$[0].senderId").doesNotExist());
        mockMvc.perform(as(deal.buyer(), withJson(post("/api/support/tickets/{id}/messages", ticketId),
                Map.of("content", "Mình không có ảnh.")))).andExpect(status().isCreated());
        // Staff see it as unread, with the real sender
        JsonNode queue = read(mockMvc.perform(as(moderator, get("/api/moderation/support-tickets").param("status", "OPEN"))));
        JsonNode ticket = queue.path("content").valueStream()
                .filter(t -> t.path("id").asLong() == ticketId).findFirst().orElseThrow();
        assertThat(ticket.path("unreadCount").asLong()).isEqualTo(1);
        mockMvc.perform(as(moderator, get("/api/moderation/support-tickets/{id}/messages", ticketId)))
                .andExpect(jsonPath("$[0].senderName").value("Buyer"));

        // Decision: hide the review; the reputation no longer counts it
        mockMvc.perform(as(moderator, withJson(post("/api/moderation/reports/{id}/resolve", reportId),
                        Map.of("action", "HIDE_REVIEW", "note", "Không có bằng chứng, người bán có ảnh giao nhận"))))
                .andExpect(jsonPath("$.status").value("RESOLVED"));
        mockMvc.perform(get("/api/users/{id}/reviews", deal.seller().id())).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/users/{id}", deal.seller().id()))
                .andExpect(jsonPath("$.reviewCount").value(0))
                .andExpect(jsonPath("$.reputationScore").doesNotExist());
        assertThat(notificationTypes(deal.buyer())).contains("REVIEW_HIDDEN");
        assertThat(notificationTypes(deal.seller())).contains("REPORT_RESOLVED");

        mockMvc.perform(as(moderator, post("/api/moderation/reviews/{id}/restore", reviewId))).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users/{id}", deal.seller().id())).andExpect(jsonPath("$.reviewCount").value(1));
        JsonNode audit = read(mockMvc.perform(as(withRole(registerVerifiedUser("Admin"), Role.ADMIN), get("/api/admin/audit-logs"))));
        assertThat(audit.path("content").findValuesAsString("action")).contains("REVIEW_HIDDEN", "REVIEW_RESTORED");
    }

    @Test
    void usersOnlyReachTheirOwnTicketsAndLimitsApply() throws Exception {
        TestUser user = registerVerifiedUser("User");
        TestUser other = registerVerifiedUser("Other");
        TestUser moderator = withRole(registerVerifiedUser("Mod"), Role.MODERATOR);

        long ticketId = read(mockMvc.perform(as(user, withJson(post("/api/support/tickets"),
                        Map.of("subject", "Hỏi về quy định", "message", "Bán sách photo có được không?"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))).path("id").asLong();

        mockMvc.perform(as(other, get("/api/support/tickets/{id}", ticketId))).andExpect(status().isNotFound());
        mockMvc.perform(as(other, withJson(post("/api/support/tickets/{id}/messages", ticketId),
                Map.of("content", "spam")))).andExpect(status().isNotFound());
        mockMvc.perform(as(user, get("/api/moderation/support-tickets"))).andExpect(status().isForbidden());

        mockMvc.perform(as(moderator, withJson(post("/api/moderation/support-tickets/{id}/messages", ticketId),
                Map.of("content", "Không được bán sách photo vi phạm bản quyền.")))).andExpect(status().isCreated());
        mockMvc.perform(as(user, get("/api/support/tickets/{id}", ticketId))).andExpect(jsonPath("$.unreadCount").value(1));
        mockMvc.perform(as(user, post("/api/support/tickets/{id}/read", ticketId))).andExpect(status().isNoContent());
        mockMvc.perform(as(user, get("/api/support/tickets/{id}", ticketId))).andExpect(jsonPath("$.unreadCount").value(0));

        mockMvc.perform(as(user, post("/api/support/tickets/{id}/close", ticketId)))
                .andExpect(jsonPath("$.status").value("CLOSED"));
        mockMvc.perform(as(user, withJson(post("/api/support/tickets/{id}/messages", ticketId),
                Map.of("content", "Thêm câu hỏi")))).andExpect(status().isConflict());

        // At most 3 open tickets; linking someone else's report is not allowed
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(as(other, withJson(post("/api/support/tickets"),
                    Map.of("subject", "Câu hỏi " + i, "message", "Nội dung")))).andExpect(status().isCreated());
        }
        mockMvc.perform(as(other, withJson(post("/api/support/tickets"),
                Map.of("subject", "Thứ 4", "message", "Nội dung")))).andExpect(status().isConflict());
        mockMvc.perform(as(user, withJson(post("/api/support/tickets"),
                Map.of("subject", "Báo cáo", "message", "x", "relatedReportId", 999_999)))).andExpect(status().isNotFound());
    }

    private List<String> notificationTypes(TestUser user) throws Exception {
        return read(mockMvc.perform(as(user, get("/api/notifications").param("size", "50"))))
                .path("content").findValuesAsString("type");
    }
}
