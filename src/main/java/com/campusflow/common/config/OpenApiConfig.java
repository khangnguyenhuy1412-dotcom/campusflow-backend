package com.campusflow.common.config;

import com.campusflow.common.web.CurrentUserId;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI description shown in Swagger UI (/swagger-ui.html). */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    static {
        // The current user comes from the JWT, it is not a request parameter clients send
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUserId.class);
    }

    @Bean
    public OpenAPI campusFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CampusFlow API")
                        .version("v1")
                        .description("""
                                Student marketplace to give away, exchange and sell study and dorm items.

                                **Authentication:** call `POST /api/auth/login`, copy `accessToken`, then click \
                                **Authorize** and paste it. Public endpoints work without a token.

                                **Errors** use RFC 9457 Problem Details: `{status, title, detail, errors?}`. \
                                Rate-limited requests return `429` with a `Retry-After` header."""))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
