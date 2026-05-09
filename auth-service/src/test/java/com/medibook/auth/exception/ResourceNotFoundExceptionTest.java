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
// ResourceNotFoundException
// ─────────────────────────────────────────────────────────────────────────────
class ResourceNotFoundExceptionTest {

    @Test
    void constructor_formatsMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "id", 5);
        assertThat(ex.getMessage()).isEqualTo("User not found with id: 5");
    }

    @Test
    void getters_returnCorrectValues() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Slot", "slotId", 99);
        assertThat(ex.getResourceName()).isEqualTo("Slot");
        assertThat(ex.getFieldName()).isEqualTo("slotId");
        assertThat(ex.getFieldValue()).isEqualTo(99);
    }

    @Test
    void stringFieldValue_isIncludedInMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "email", "nobody@x.com");
        assertThat(ex.getMessage()).contains("nobody@x.com");
    }

    @Test
    void isRuntimeException() {
        assertThat(new ResourceNotFoundException("X", "y", 1)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void canBeThrown() {
        assertThatThrownBy(() -> { throw new ResourceNotFoundException("Appointment", "id", 10); })
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Appointment not found with id: 10");
    }
}


