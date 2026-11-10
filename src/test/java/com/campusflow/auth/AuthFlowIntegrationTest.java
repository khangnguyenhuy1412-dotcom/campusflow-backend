package com.campusflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusflow.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class AuthFlowIntegrationTest extends IntegrationTest {

    @Test
    void onlyStudentEmailsCanRegister() throws Exception {
        mockMvc.perform(withJson(post("/api/auth/register"),
                        Map.of("email", "someone@gmail.com", "password", PASSWORD, "fullName", "X")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerVerifyLoginAndUseToken() throws Exception {
        TestUser user = registerUser("An");

        // Unverified: can browse but not post
        mockMvc.perform(as(user, withJson(post("/api/items"), Map.of())))
                .andExpect(status().isForbidden());

        TestUser verified = registerVerifiedUserFrom(user);
        mockMvc.perform(as(verified, get("/api/users/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.email()))
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void refreshTokenRotationAndReuseDetection() throws Exception {
        TestUser user = registerVerifiedUser("Binh");

        JsonNode refreshed = read(mockMvc.perform(withJson(post("/api/auth/refresh"),
                        Map.of("refreshToken", user.refreshToken())))
                .andExpect(status().isOk()));
        String newRefreshToken = refreshed.path("refreshToken").asString();
        assertThat(newRefreshToken).isNotEqualTo(user.refreshToken());

        // Reusing the old token looks like theft: it fails and revokes every session
        mockMvc.perform(withJson(post("/api/auth/refresh"), Map.of("refreshToken", user.refreshToken())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(withJson(post("/api/auth/refresh"), Map.of("refreshToken", newRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotAndResetPassword() throws Exception {
        TestUser user = registerVerifiedUser("Chi");

        mockMvc.perform(withJson(post("/api/auth/forgot-password"), Map.of("email", user.email())))
                .andExpect(status().isAccepted());
        String code = mail.awaitCode(user.email(), "mật khẩu");

        mockMvc.perform(withJson(post("/api/auth/reset-password"),
                        Map.of("email", user.email(), "code", code, "newPassword", "new-password-1")))
                .andExpect(status().isNoContent());
        mockMvc.perform(withJson(post("/api/auth/login"), Map.of("email", user.email(), "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(withJson(post("/api/auth/login"), Map.of("email", user.email(), "password", "new-password-1")))
                .andExpect(status().isOk());
        // Old sessions were revoked by the password change
        mockMvc.perform(withJson(post("/api/auth/refresh"), Map.of("refreshToken", user.refreshToken())))
                .andExpect(status().isUnauthorized());
    }

    private TestUser registerVerifiedUserFrom(TestUser user) throws Exception {
        String code = mail.awaitCode(user.email(), "xác thực");
        JsonNode body = read(mockMvc.perform(withJson(post("/api/auth/verify-email"),
                        Map.of("email", user.email(), "code", code)))
                .andExpect(status().isOk()));
        return new TestUser(user.id(), user.email(), body.path("accessToken").asString(),
                body.path("refreshToken").asString());
    }
}
