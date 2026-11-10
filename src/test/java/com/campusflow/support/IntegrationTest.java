package com.campusflow.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base class for integration tests: the whole application on a random port, a real MySQL in
 * Docker with all Flyway migrations, and emails captured in memory. Skipped when Docker is not
 * available. All subclasses share one Spring context (and one container), so keep the
 * configuration here identical for every test.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.jwt.secret=integration-test-secret-integration-test-secret",
                "app.rate-limit.enabled=false",
                "app.storage.local-root=target/it-uploads",
                "app.housekeeping.cron=-",
                "spring.jpa.show-sql=false"
        })
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, IntegrationTest.MailConfig.class})
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTest {

    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger();
    private static final long RUN = System.currentTimeMillis();
    protected static final String PASSWORD = "password123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JsonMapper jsonMapper;

    @Autowired
    protected RecordingMailSender mail;

    @TestConfiguration(proxyBeanMethods = false)
    static class MailConfig {

        @Bean
        @Primary
        RecordingMailSender recordingMailSender() {
            return new RecordingMailSender();
        }
    }

    public record TestUser(Long id, String email, String accessToken, String refreshToken) {
    }

    /** Registers a new student (unique email per call) without verifying the email. */
    protected TestUser registerUser(String name) throws Exception {
        String email = "%s.%d.%d@student.hcmus.edu.vn".formatted(
                name.toLowerCase(Locale.ROOT), RUN, USER_SEQUENCE.incrementAndGet());
        JsonNode body = read(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD, "fullName", name))))
                .andExpect(status().isCreated()));
        return toUser(body);
    }

    /** Registers a student and verifies the email with the code from the (captured) email. */
    protected TestUser registerVerifiedUser(String name) throws Exception {
        TestUser user = registerUser(name);
        String code = mail.awaitCode(user.email(), "xác thực");
        JsonNode body = read(mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.email(), "code", code))))
                .andExpect(status().isOk()));
        return toUser(body);
    }

    /** Adds the user's access token to a request. */
    protected static MockHttpServletRequestBuilder as(TestUser user, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + user.accessToken());
    }

    /** Request with a JSON body. */
    protected MockHttpServletRequestBuilder withJson(MockHttpServletRequestBuilder request, Object body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(json(body));
    }

    protected String json(Object value) {
        return jsonMapper.writeValueAsString(value);
    }

    protected JsonNode read(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private static TestUser toUser(JsonNode authResponse) {
        return new TestUser(
                authResponse.path("user").path("id").asLong(),
                authResponse.path("user").path("email").asString(),
                authResponse.path("accessToken").asString(),
                authResponse.path("refreshToken").asString());
    }
}
