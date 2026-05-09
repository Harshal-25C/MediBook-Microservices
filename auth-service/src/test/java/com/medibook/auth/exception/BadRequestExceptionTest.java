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
// BadRequestException
// ─────────────────────────────────────────────────────────────────────────────
class BadRequestExceptionTest {

    @Test
    void constructor_setsMessage() {
        BadRequestException ex = new BadRequestException("Invalid data");
        assertThat(ex.getMessage()).isEqualTo("Invalid data");
    }

    @Test
    void isRuntimeException() {
        assertThat(new BadRequestException("msg")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void message_isPreservedExactly() {
        String msg = "Cannot book a slot that is already booked.";
        assertThat(new BadRequestException(msg).getMessage()).isEqualTo(msg);
    }

    @Test
    void canBeThrown() {
        assertThatThrownBy(() -> { throw new BadRequestException("bad"); })
                .isInstanceOf(BadRequestException.class)
                .hasMessage("bad");
    }
}

