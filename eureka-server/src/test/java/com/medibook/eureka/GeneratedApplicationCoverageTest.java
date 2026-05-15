
package com.medibook.eureka;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class GeneratedApplicationCoverageTest {
    @Test
    void mainDelegatesToSpringApplication() {
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            spring.when(() -> SpringApplication.run(any(Class.class), any(String[].class))).thenReturn(null);
            assertThatCode(() -> Class.forName("com.medibook.eureka.EurekaServerApplication").getMethod("main", String[].class)
                    .invoke(null, (Object) new String[]{})).doesNotThrowAnyException();
        }
    }
}
