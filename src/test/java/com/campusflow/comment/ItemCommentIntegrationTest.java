package com.campusflow.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusflow.support.IntegrationTest;
import com.campusflow.user.entity.Role;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Public Q&A under a listing: comments, one level of replies, notifications and moderation. */
class ItemCommentIntegrationTest extends IntegrationTest {

    private long createItem(TestUser owner) throws Exception {
        return read(mockMvc.perform(as(owner, withJson(post("/api/items"), Map.of(
                "categoryId", 3, "title", "Tai nghe Bluetooth", "description", "Pin tốt",
                "itemType", "SALE", "price", 150000, "condition", "LIKE_NEW"))))
                .andExpect(status().isCreated())).path("id").asLong();
    }

    private long comment(TestUser author, long itemId, String content, Long parentId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        body.put("parentId", parentId);
        return read(mockMvc.perform(as(author, withJson(post("/api/items/{id}/comments", itemId), body)))
                .andExpect(status().isCreated())).path("id").asLong();
    }

    @Test
    void questionAndSellerAnswerWithNotifications() throws Exception {
        TestUser seller = registerVerifiedUser("Seller");
        TestUser buyer = registerVerifiedUser("Buyer");
        long itemId = createItem(seller);

        long questionId = comment(buyer, itemId, "Còn bảo hành không bạn?", null);
        long answerId = comment(seller, itemId, "Còn 6 tháng bạn nhé", questionId);

        // Anyone can read the thread; the seller's answer has the badge
        mockMvc.perform(get("/api/items/{id}/comments", itemId))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Còn bảo hành không bạn?"))
                .andExpect(jsonPath("$.content[0].seller").value(false))
                .andExpect(jsonPath("$.content[0].replies[0].id").value(answerId))
                .andExpect(jsonPath("$.content[0].replies[0].seller").value(true));

        assertThat(notificationTypes(seller)).containsExactly("ITEM_COMMENTED");
        assertThat(notificationTypes(buyer)).containsExactly("COMMENT_REPLIED");

        // Replies are one level deep
        mockMvc.perform(as(buyer, withJson(post("/api/items/{id}/comments", itemId),
                Map.of("content", "Cảm ơn", "parentId", answerId)))).andExpect(status().isBadRequest());
    }

    @Test
    void whoMayCommentAndDelete() throws Exception {
        TestUser seller = registerVerifiedUser("Seller");
        TestUser buyer = registerVerifiedUser("Buyer");
        TestUser stranger = registerVerifiedUser("Stranger");
        TestUser unverified = registerUser("Unverified");
        long itemId = createItem(seller);

        mockMvc.perform(as(unverified, withJson(post("/api/items/{id}/comments", itemId), Map.of("content", "Hi"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(seller, post("/api/blocks/{id}", stranger.id()))).andExpect(status().isNoContent());
        mockMvc.perform(as(stranger, withJson(post("/api/items/{id}/comments", itemId), Map.of("content", "Hi"))))
                .andExpect(status().isForbidden());

        long own = comment(buyer, itemId, "Giá bao nhiêu?", null);
        long other = comment(buyer, itemId, "Ship không?", null);
        mockMvc.perform(as(stranger, delete("/api/items/{id}/comments/{c}", itemId, own))).andExpect(status().isForbidden());
        mockMvc.perform(as(buyer, delete("/api/items/{id}/comments/{c}", itemId, own))).andExpect(status().isNoContent());
        // The item owner can tidy up their listing too
        mockMvc.perform(as(seller, delete("/api/items/{id}/comments/{c}", itemId, other))).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/items/{id}/comments", itemId))
                .andExpect(jsonPath("$.content[0].state").value("DELETED"))
                .andExpect(jsonPath("$.content[0].content").doesNotExist())
                .andExpect(jsonPath("$.content[0].author").doesNotExist());

        // Comments are hidden together with a hidden listing
        mockMvc.perform(as(seller, post("/api/items/{id}/hide", itemId))).andExpect(status().isOk());
        mockMvc.perform(get("/api/items/{id}/comments", itemId)).andExpect(status().isNotFound());
        mockMvc.perform(as(seller, get("/api/items/{id}/comments", itemId))).andExpect(status().isOk());
    }

    @Test
    void reportedCommentIsHiddenByAModerator() throws Exception {
        TestUser seller = registerVerifiedUser("Seller");
        TestUser troll = registerVerifiedUser("Troll");
        TestUser moderator = withRole(registerVerifiedUser("Mod"), Role.MODERATOR);
        long itemId = createItem(seller);
        long commentId = comment(troll, itemId, "Hàng giả đấy đừng mua", null);

        mockMvc.perform(as(troll, withJson(post("/api/reports"),
                Map.of("targetType", "COMMENT", "targetId", commentId, "reason", "SPAM")))).andExpect(status().isBadRequest());
        long reportId = read(mockMvc.perform(as(seller, withJson(post("/api/reports"),
                Map.of("targetType", "COMMENT", "targetId", commentId, "reason", "HARASSMENT"))))
                .andExpect(status().isCreated())).path("id").asLong();
        mockMvc.perform(as(moderator, get("/api/moderation/reports/{id}", reportId)))
                .andExpect(jsonPath("$.targetCommentContent").value("Hàng giả đấy đừng mua"))
                .andExpect(jsonPath("$.targetUser.id").value(troll.id()));

        mockMvc.perform(as(moderator, withJson(post("/api/moderation/reports/{id}/resolve", reportId),
                Map.of("action", "HIDE_COMMENT", "note", "Vu khống")))).andExpect(status().isOk());
        mockMvc.perform(get("/api/items/{id}/comments", itemId))
                .andExpect(jsonPath("$.content[0].state").value("HIDDEN"))
                .andExpect(jsonPath("$.content[0].content").doesNotExist());

        mockMvc.perform(as(moderator, post("/api/moderation/comments/{id}/restore", commentId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/items/{id}/comments", itemId)).andExpect(jsonPath("$.content[0].state").value("VISIBLE"));
    }

    private List<String> notificationTypes(TestUser user) throws Exception {
        return read(mockMvc.perform(as(user, get("/api/notifications")))).path("content").findValuesAsString("type");
    }
}
