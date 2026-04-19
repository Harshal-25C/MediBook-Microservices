package com.medibook.auth.exception;
 
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
 
import java.util.List;
import java.util.Map;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
 
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler — Unit Tests")
class GlobalExceptionHandlerTest {
 
    @InjectMocks
    private GlobalExceptionHandler handler;
 
    // ── ResourceNotFoundException → 404 ──────────────────────────────────────
 
    @Nested
    @DisplayName("handleNotFound() — ResourceNotFoundException → 404")
    class NotFoundTests {
 
        @Test
        @DisplayName("returns 404 status")
        void shouldReturn404() {
            var ex = new ResourceNotFoundException("User not found: 99");
            var response = handler.handleNotFound(ex);
 
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
 
        @Test
        @DisplayName("error body contains correct message and status=404")
        void shouldContainCorrectMessageAndStatus() {
            var ex = new ResourceNotFoundException("User not found: 99");
            var response = handler.handleNotFound(ex);
            var body = response.getBody();
 
            assertThat(body).isNotNull();
            assertThat(body.status()).isEqualTo(404);
            assertThat(body.message()).isEqualTo("User not found: 99");
        }
 
        @Test
        @DisplayName("timestamp is not null")
        void shouldHaveNonNullTimestamp() {
            var ex = new ResourceNotFoundException("Not found");
            var body = handler.handleNotFound(ex).getBody();
 
            assertThat(body).isNotNull();
            assertThat(body.timestamp()).isNotNull();
        }
    }
 
    // ── IllegalArgumentException → 400 ───────────────────────────────────────
 
    @Nested
    @DisplayName("handleBadRequest() — IllegalArgumentException → 400")
    class BadRequestTests {
 
        @Test
        @DisplayName("returns 400 status")
        void shouldReturn400() {
            var ex = new IllegalArgumentException("Invalid credentials");
            var response = handler.handleBadRequest(ex);
 
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
 
        @Test
        @DisplayName("body status field equals 400")
        void bodyStatusEquals400() {
            var ex = new IllegalArgumentException("Email is already registered.");
            var body = handler.handleBadRequest(ex).getBody();
 
            assertThat(body).isNotNull();
            assertThat(body.status()).isEqualTo(400);
        }
 
        @Test
        @DisplayName("body message matches exception message")
        void bodyMessageMatchesException() {
            var ex = new IllegalArgumentException("Current password is incorrect");
            var body = handler.handleBadRequest(ex).getBody();
 
            assertThat(body).isNotNull();
            assertThat(body.message()).isEqualTo("Current password is incorrect");
        }
    }
 
    // ── IllegalStateException → 409 ──────────────────────────────────────────
 
    @Nested
    @DisplayName("handleConflict() — IllegalStateException → 409")
    class ConflictTests {
 
        @Test
        @DisplayName("returns 409 status")
        void shouldReturn409() {
            var ex = new IllegalStateException("Account is deactivated");
            var response = handler.handleConflict(ex);
 
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
 
        @Test
        @DisplayName("body status field equals 409")
        void bodyStatusEquals409() {
            var ex = new IllegalStateException("Email not verified.");
            var body = handler.handleConflict(ex).getBody();
 
            assertThat(body).isNotNull();
            assertThat(body.status()).isEqualTo(409);
        }
 
        @Test
        @DisplayName("body message matches exception message")
        void bodyMessageMatchesException() {
            String msg = "Admin accounts cannot be deleted through this endpoint";
            var ex = new IllegalStateException(msg);
            var body = handler.handleConflict(ex).getBody();
 
            assertThat(body).isNotNull();
            assertThat(body.message()).isEqualTo(msg);
        }
    }
 
    // ── MethodArgumentNotValidException → 400 with field errors ──────────────
 
    @Nested
    @DisplayName("handleValidation() — MethodArgumentNotValidException → 400 field errors")
    class ValidationTests {
 
        private MethodArgumentNotValidException buildValidationException(
                String field, String defaultMsg) {
            FieldError fieldError = new FieldError("obj", field, defaultMsg);
            BindingResult bindingResult = mock(BindingResult.class);
            given(bindingResult.getAllErrors()).willReturn(List.of(fieldError));
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            given(ex.getBindingResult()).willReturn(bindingResult);
            return ex;
        }
 
        @Test
        @DisplayName("returns 400 status")
        void shouldReturn400() {
            var ex = buildValidationException("email", "must not be blank");
            var response = handler.handleValidation(ex);
 
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
 
        @Test
        @DisplayName("response body contains 'status' = 400")
        void bodyContainsStatus400() {
            var ex = buildValidationException("email", "must be a valid email");
            var body = handler.handleValidation(ex).getBody();
 
            assertThat(body).isNotNull();
            assertThat(body.get("status")).isEqualTo(400);
        }
 
        @Test
        @DisplayName("response body 'errors' map contains field→message entry")
        @SuppressWarnings("unchecked")
        void bodyContainsFieldErrorEntry() {
            var ex = buildValidationException("email", "must be a valid email");
            var body = handler.handleValidation(ex).getBody();
 
            assertThat(body).isNotNull();
            Map<String, String> errors = (Map<String, String>) body.get("errors");
            assertThat(errors).containsEntry("email", "must be a valid email");
        }
 
        @Test
        @DisplayName("response body 'timestamp' key exists")
        void bodyContainsTimestamp() {
            var ex = buildValidationException("password", "must be at least 8 characters");
            var body = handler.handleValidation(ex).getBody();
 
            assertThat(body).isNotNull();
            assertThat(body).containsKey("timestamp");
        }
 
        @Test
        @DisplayName("multiple validation errors are all included in errors map")
        @SuppressWarnings("unchecked")
        void bodyContainsMultipleFieldErrors() {
            FieldError err1 = new FieldError("obj", "email", "must not be blank");
            FieldError err2 = new FieldError("obj", "password", "must be at least 8 characters");
            BindingResult bindingResult = mock(BindingResult.class);
            given(bindingResult.getAllErrors()).willReturn(List.of(err1, err2));
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            given(ex.getBindingResult()).willReturn(bindingResult);
 
            var body = handler.handleValidation(ex).getBody();
            assertThat(body).isNotNull();
            Map<String, String> errors = (Map<String, String>) body.get("errors");
            assertThat(errors).hasSize(2);
            assertThat(errors).containsKey("email");
            assertThat(errors).containsKey("password");
        }
    }
}