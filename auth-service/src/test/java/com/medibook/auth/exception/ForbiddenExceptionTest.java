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
// ForbiddenException
// ─────────────────────────────────────────────────────────────────────────────
class ForbiddenExceptionTest {

    @Test
    void constructor_setsMessage() {
        ForbiddenException ex = new ForbiddenException("Access denied");
        assertThat(ex.getMessage()).isEqualTo("Access denied");
    }

    @Test
    void isRuntimeException() {
        assertThat(new ForbiddenException("msg")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void canBeThrown() {
        assertThatThrownBy(() -> { throw new ForbiddenException("No permission"); })
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("No permission");
    }
}

