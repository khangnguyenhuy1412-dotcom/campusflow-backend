package com.campusflow.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusflow.support.IntegrationTest;
import com.campusflow.user.entity.Role;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Reports, moderation, bans, blocks and the admin API against the real database. */
class TrustSafetyIntegrationTest extends IntegrationTest {

    private long createItem(TestUser owner, String title) throws Exception {
        return read(mockMvc.perform(as(owner, withJson(post("/api/items"), Map.of(
                        "categoryId", 6, "title", title, "description", "Mô tả", "itemType", "GIVEAWAY",
                        "condition", "USED"))))
                .andExpect(status().isCreated())).path("id").asLong();
    }

    private long report(TestUser reporter, String type, long targetId) throws Exception {
        return read(mockMvc.perform(as(reporter, withJson(post("/api/reports"),
                        Map.of("targetType", type, "targetId", targetId, "reason", "SCAM", "details", "Lừa đảo"))))
                .andExpect(status().isCreated())).path("id").asLong();
    }

    @Test
    void reportedListingIsRemovedAndEveryoneIsTold() throws Exception {
        TestUser owner = registerVerifiedUser("Owner");
        TestUser reporter1 = registerVerifiedUser("Reporter1");
        TestUser reporter2 = registerVerifiedUser("Reporter2");
        TestUser moderator = withRole(registerVerifiedUser("Mod"), Role.MODERATOR);
        long itemId = createItem(owner, "iPhone giá 100k");
        long requestId = read(mockMvc.perform(as(reporter2, withJson(post("/api/exchange-requests"),
                Map.of("itemId", itemId))))).path("id").asLong();

        long reportId = report(reporter1, "ITEM", itemId);
        report(reporter2, "ITEM", itemId);
        mockMvc.perform(as(reporter1, withJson(post("/api/reports"),
                        Map.of("targetType", "ITEM", "targetId", itemId, "reason", "SPAM"))))
                .andExpect(status().isConflict());
        mockMvc.perform(as(owner, withJson(post("/api/reports"),
                        Map.of("targetType", "ITEM", "targetId", itemId, "reason", "SPAM"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(as(reporter1, get("/api/moderation/reports"))).andExpect(status().isForbidden());
        mockMvc.perform(as(moderator, withJson(post("/api/moderation/reports/{id}/resolve", reportId),
                        Map.of("action", "REMOVE_ITEM", "note", "Giá ảo, nghi lừa đảo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // Listing gone for everyone, the owner cannot bring it back
        mockMvc.perform(get("/api/items/{id}", itemId)).andExpect(status().isNotFound());
        mockMvc.perform(as(owner, post("/api/items/{id}/show", itemId))).andExpect(status().isConflict());
        // The open request was cancelled; owner, requester and both reporters were notified
        mockMvc.perform(as(reporter2, get("/api/exchange-requests/{id}", requestId)))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(notificationTypes(owner)).contains("ITEM_REMOVED");
        assertThat(notificationTypes(reporter2)).contains("EXCHANGE_CANCELLED", "REPORT_RESOLVED");
        assertThat(notificationTypes(reporter1)).contains("REPORT_RESOLVED");
        assertThat(mail.awaitEmail(owner.email(), "đã bị gỡ").getText()).contains("Giá ảo");
        // Both reports were closed by the one decision
        mockMvc.perform(as(moderator, get("/api/moderation/reports").param("status", "OPEN")))
                .andExpect(jsonPath("$.content[?(@.targetItemId == " + itemId + ")]").isEmpty());
    }

    @Test
    void banTakesEffectImmediatelyAndCanBeLifted() throws Exception {
        TestUser user = registerVerifiedUser("Spammer");
        TestUser admin = withRole(registerVerifiedUser("Admin"), Role.ADMIN);
        mockMvc.perform(as(user, get("/api/users/me"))).andExpect(status().isOk());

        mockMvc.perform(as(admin, withJson(post("/api/admin/users/{id}/ban", user.id()), Map.of("note", "Spam"))))
                .andExpect(status().isNoContent());

        // The access token is still unexpired, but the account is refused right away
        mockMvc.perform(as(user, get("/api/users/me")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Account is disabled"));
        mockMvc.perform(withJson(post("/api/auth/login"), Map.of("email", user.email(), "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(withJson(post("/api/auth/refresh"), Map.of("refreshToken", user.refreshToken())))
                .andExpect(status().isUnauthorized());
        assertThat(mail.awaitEmail(user.email(), "bị khóa").getText()).contains("Spam");
        mockMvc.perform(get("/api/users/{id}", user.id())).andExpect(status().isNotFound());

        mockMvc.perform(as(admin, post("/api/admin/users/{id}/unban", user.id()))).andExpect(status().isNoContent());
        mockMvc.perform(withJson(post("/api/auth/login"), Map.of("email", user.email(), "password", PASSWORD)))
                .andExpect(status().isOk());

        JsonNode log = read(mockMvc.perform(as(admin, get("/api/admin/audit-logs"))));
        assertThat(log.path("content").findValuesAsString("action")).contains("USER_BANNED", "USER_UNBANNED");
    }

    @Test
    void moderatorsCannotBanAdminsOrOtherModerators() throws Exception {
        TestUser moderator = withRole(registerVerifiedUser("Mod"), Role.MODERATOR);
        TestUser otherModerator = withRole(registerVerifiedUser("Mod2"), Role.MODERATOR);
        TestUser admin = withRole(registerVerifiedUser("Admin"), Role.ADMIN);

        mockMvc.perform(as(moderator, withJson(post("/api/moderation/users/{id}/ban", admin.id()), Map.of())))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(moderator, withJson(post("/api/moderation/users/{id}/ban", otherModerator.id()), Map.of())))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(moderator, withJson(post("/api/moderation/users/{id}/ban", moderator.id()), Map.of())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blockedUsersCannotContactEachOther() throws Exception {
        TestUser seller = registerVerifiedUser("Seller");
        TestUser buyer = registerVerifiedUser("Buyer");
        long itemId = createItem(seller, "Bàn học gấp");

        mockMvc.perform(as(seller, post("/api/blocks/{id}", buyer.id()))).andExpect(status().isNoContent());

        // Blocking works in both directions
        mockMvc.perform(as(buyer, withJson(post("/api/conversations"), Map.of("itemId", itemId))))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(buyer, withJson(post("/api/exchange-requests"), Map.of("itemId", itemId))))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(seller, get("/api/blocks")))
                .andExpect(jsonPath("$.content[0].id").value(buyer.id()));

        mockMvc.perform(as(seller, delete("/api/blocks/{id}", buyer.id()))).andExpect(status().isNoContent());
        mockMvc.perform(as(buyer, withJson(post("/api/conversations"), Map.of("itemId", itemId))))
                .andExpect(status().isOk());
    }

    @Test
    void adminDashboardAndRoleManagement() throws Exception {
        TestUser admin = withRole(registerVerifiedUser("Admin"), Role.ADMIN);
        TestUser student = registerVerifiedUser("Student");
        createItem(student, "Quạt bàn");

        mockMvc.perform(as(admin, get("/api/admin/stats")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.users").isNumber())
                .andExpect(jsonPath("$.last30Days.length()").value(30))
                .andExpect(jsonPath("$.itemsByStatus.AVAILABLE").isNumber());
        mockMvc.perform(as(admin, get("/api/admin/users").param("query", student.email())))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].active").value(true));
        mockMvc.perform(as(student, get("/api/admin/stats"))).andExpect(status().isForbidden());

        mockMvc.perform(as(admin, withJson(put("/api/admin/users/{id}/moderator", student.id()), Map.of("moderator", true))))
                .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.hasItem(Role.MODERATOR)));
        TestUser promoted = login(student);
        mockMvc.perform(as(promoted, get("/api/moderation/reports"))).andExpect(status().isOk());
    }

    private java.util.List<String> notificationTypes(TestUser user) throws Exception {
        return read(mockMvc.perform(as(user, get("/api/notifications")))).path("content").findValuesAsString("type");
    }
}
