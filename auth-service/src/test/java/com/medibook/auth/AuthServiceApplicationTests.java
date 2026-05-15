package com.medibook.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.medibook.AuthServiceApplication;

class AuthServiceApplicationTests {

    @Test
    void applicationClassExists() {
        assertThat(AuthServiceApplication.class).isNotNull();
    }
}
