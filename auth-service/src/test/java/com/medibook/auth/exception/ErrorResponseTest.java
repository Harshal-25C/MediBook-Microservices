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
// ErrorResponse
// ─────────────────────────────────────────────────────────────────────────────
class ErrorResponseTest {

    @Test
    void allArgsConstructor_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        ErrorResponse response = new ErrorResponse(404, "Not Found", "User not found", "/auth/profile/99", now);

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getError()).isEqualTo("Not Found");
        assertThat(response.getMessage()).isEqualTo("User not found");
        assertThat(response.getPath()).isEqualTo("/auth/profile/99");
        assertThat(response.getTimestamp()).isEqualTo(now);
    }

    @Test
    void noArgsConstructor_createsEmpty() {
        ErrorResponse response = new ErrorResponse();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getStatus()).isEqualTo(0);
    }

    @Test
    void setters_updateFields() {
        ErrorResponse response = new ErrorResponse();
        LocalDateTime now = LocalDateTime.now();
        response.setStatus(500);
        response.setError("Internal Server Error");
        response.setMessage("Something went wrong");
        response.setPath("/auth/login");
        response.setTimestamp(now);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getError()).isEqualTo("Internal Server Error");
        assertThat(response.getMessage()).isEqualTo("Something went wrong");
        assertThat(response.getPath()).isEqualTo("/auth/login");
        assertThat(response.getTimestamp()).isEqualTo(now);
    }

    @Test
    void equalResponses_areEqual() {
        LocalDateTime ts = LocalDateTime.now();
        ErrorResponse r1 = new ErrorResponse(400, "Bad Request", "msg", "/path", ts);
        ErrorResponse r2 = new ErrorResponse(400, "Bad Request", "msg", "/path", ts);
        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void toString_containsStatusCode() {
        ErrorResponse response = new ErrorResponse(403, "Forbidden", "No access", "/admin", LocalDateTime.now());
        assertThat(response.toString()).contains("403");
    }
}

