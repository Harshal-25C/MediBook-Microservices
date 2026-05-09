package com.medibook.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// ─────────────────────────────────────────────────────────────────────────────
// DuplicateResourceException
// ─────────────────────────────────────────────────────────────────────────────
class DuplicateResourceExceptionTest {

    @Test
    void threeArgConstructor_formatsMessage() {
        DuplicateResourceException ex = new DuplicateResourceException("User", "email", "a@b.com");
        assertThat(ex.getMessage()).isEqualTo("User already exists with email: a@b.com");
    }

    @Test
    void singleArgConstructor_usesMessageDirectly() {
        DuplicateResourceException ex = new DuplicateResourceException("Payment already exists for appointment: 7");
        assertThat(ex.getMessage()).isEqualTo("Payment already exists for appointment: 7");
    }

    @Test
    void isRuntimeException() {
        assertThat(new DuplicateResourceException("msg")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void numericFieldValue_isIncludedInMessage() {
        DuplicateResourceException ex = new DuplicateResourceException("Provider profile", "userId", 42);
        assertThat(ex.getMessage()).contains("42");
    }

    @Test
    void canBeThrown() {
        assertThatThrownBy(() -> { throw new DuplicateResourceException("User", "email", "x@x.com"); })
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("x@x.com");
    }
}

