package com.medibook.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

// ─────────────────────────────────────────────────────────────────────────────
// OpenApiConfig
// ─────────────────────────────────────────────────────────────────────────────
class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void customOpenAPI_returnsNonNull() {
        OpenAPI api = openApiConfig.customOpenAPI();
        assertThat(api).isNotNull();
    }

    @Test
    void customOpenAPI_hasCorrectTitle() {
        OpenAPI api = openApiConfig.customOpenAPI();
        assertThat(api.getInfo().getTitle()).isEqualTo("MediBook — Auth Service API");
    }

    @Test
    void customOpenAPI_hasVersion() {
        OpenAPI api = openApiConfig.customOpenAPI();
        assertThat(api.getInfo().getVersion()).isEqualTo("v1.0");
    }

    @Test
    void customOpenAPI_hasDescription() {
        OpenAPI api = openApiConfig.customOpenAPI();
        assertThat(api.getInfo().getDescription()).isNotBlank();
    }

    @Test
    void customOpenAPI_hasContactInfo() {
        OpenAPI api = openApiConfig.customOpenAPI();
        assertThat(api.getInfo().getContact()).isNotNull();
        assertThat(api.getInfo().getContact().getName()).isEqualTo("MediBook Team");
        assertThat(api.getInfo().getContact().getEmail()).isEqualTo("dev@medibook.com");
    }

    @Test
    void customOpenAPI_hasBearerAuthSecurityScheme() {
        OpenAPI api = openApiConfig.customOpenAPI();
        assertThat(api.getComponents()).isNotNull();
        assertThat(api.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
    }

    @Test
    void customOpenAPI_bearerSchemeIsHttp() {
        OpenAPI api = openApiConfig.customOpenAPI();
        var scheme = api.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(scheme.getType()).isEqualTo(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
    }

    @Test
    void customOpenAPI_bearerFormatIsJWT() {
        OpenAPI api = openApiConfig.customOpenAPI();
        var scheme = api.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
    }

    @Test
    void customOpenAPI_hasSecurityRequirement() {
        OpenAPI api = openApiConfig.customOpenAPI();
        assertThat(api.getSecurity()).isNotNull().isNotEmpty();
    }

    @Test
    void customOpenAPI_doesNotThrow() {
        assertThatCode(openApiConfig::customOpenAPI).doesNotThrowAnyException();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CorsConfig
// ─────────────────────────────────────────────────────────────────────────────
class CorsConfigTest {

    @Test
    void corsConfig_instantiatesWithoutError() {
        assertThatCode(CorsConfig::new).doesNotThrowAnyException();
    }

    @Test
    void corsConfig_isNotNull() {
        CorsConfig config = new CorsConfig();
        assertThat(config).isNotNull();
    }
}
