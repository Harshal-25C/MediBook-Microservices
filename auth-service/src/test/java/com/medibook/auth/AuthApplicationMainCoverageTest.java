package com.medibook.auth;

import static org.mockito.Mockito.mockStatic;

import com.medibook.AuthServiceApplication;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class AuthApplicationMainCoverageTest {

    @Test
    void mainDelegatesToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            String[] args = {"--spring.main.web-application-type=none"};
            AuthServiceApplication.main(args);
            mocked.verify(() -> SpringApplication.run(AuthServiceApplication.class, args));
        }
    }
}