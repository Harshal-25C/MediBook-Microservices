
package com.medibook.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.medibook.payment.config.OpenApiConfig;
import org.junit.jupiter.api.Test;

class GeneratedPaymentConfigCoverageTest {
    @Test
    void openApiContainsBearerSecurityScheme() {
        var api = new OpenApiConfig().customOpenAPI();
        assertThat(api.getInfo().getTitle()).contains("MediBook");
        assertThat(api.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(api.getSecurity()).isNotEmpty();
    }
}
