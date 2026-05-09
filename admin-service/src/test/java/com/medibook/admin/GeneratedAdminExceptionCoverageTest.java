
package com.medibook.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.medibook.admin.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GeneratedAdminExceptionCoverageTest {
    @Test
    void validationAndGeneralHandlersReturnBodies() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult result = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(result);
        when(result.getFieldErrors()).thenReturn(List.of(new FieldError("admin", "email", "bad")));

        assertThat(handler.handleValidation(ex).getStatusCode().value()).isEqualTo(400);
        assertThat(handler.handleGeneral(new Exception("boom")).getStatusCode().value()).isEqualTo(500);
    }
}
