package com.campusflow.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusflow.auth.controller.AuthController;
import com.campusflow.auth.dto.AuthResponse;
import com.campusflow.auth.service.AuthService;
import com.campusflow.user.controller.UserController;
import com.campusflow.user.dto.UserResponse;
import com.campusflow.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = {AuthController.class, UserController.class},
        properties = {
                "app.jwt.secret=test-secret-test-secret-test-secret-123",
                "app.jwt.issuer=campusflow",
                "app.jwt.access-token-ttl=15m",
                "app.jwt.refresh-token-ttl=7d"
        })
@Import({SecurityConfig.class, JwtConfig.class, JwtService.class, SecurityErrorHandler.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private String bearer(String... roles) {
        return "Bearer " + jwtService.generateAccessToken(1L, "an@student.edu.vn", true, List.of(roles));
    }

    private String unverifiedBearer() {
        return "Bearer " + jwtService.generateAccessToken(1L, "an@student.edu.vn", false, List.of("ROLE_STUDENT"));
    }

    @Test
    void unverifiedStudentCannotCreateItems() throws Exception {
        mockMvc.perform(post("/api/items").header("Authorization", unverifiedBearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unverifiedStudentCanStillBrowseItems() throws Exception {
        // No items controller yet: 404 means security let the request through
        mockMvc.perform(get("/api/items").header("Authorization", unverifiedBearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void verifiedStudentPassesItemWriteRule() throws Exception {
        mockMvc.perform(post("/api/items").header("Authorization", bearer("ROLE_STUDENT")))
                .andExpect(status().isNotFound());
    }

    @Test
    void verifyEmailIsPublicAndValidatesCode() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"an@student.edu.vn\",\"code\":\"12ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").value("must be a 6-digit code"));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Authentication is required"));
    }

    @Test
    void protectedEndpointWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid or expired access token"));
    }

    @Test
    void protectedEndpointWithValidTokenReturns200() throws Exception {
        when(userService.getById(1L)).thenReturn(new UserResponse(
                1L, "an@student.edu.vn", "An", null, null, null, null, null, false, List.of("ROLE_STUDENT"), null));

        mockMvc.perform(get("/api/users/me").header("Authorization", bearer("ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("an@student.edu.vn"));
    }

    @Test
    void adminEndpointWithStudentTokenReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer("ROLE_STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminEndpointWithAdminTokenPassesSecurity() throws Exception {
        // No admin controller exists yet, so getting past security means a 404
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void loginIsPublic() throws Exception {
        when(authService.login(any())).thenReturn(AuthResponse.bearer("access", "refresh", 900, null));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"an@student.edu.vn\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void registerWithInvalidBodyReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"123\",\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }
}
