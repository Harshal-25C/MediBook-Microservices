
package com.medibook.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GeneratedExceptionHandlerCoverageTest {

    @Test
    void everyExceptionHandlerMethodBuildsAResponse() throws Exception {
        Object handler = Class.forName("com.medibook.provider.exception.GlobalExceptionHandler")
                .getDeclaredConstructor().newInstance();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/unit-test");

        int handled = 0;
        for (Method method : handler.getClass().getDeclaredMethods()) {
            if (!ResponseEntity.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            Object[] args = new Object[method.getParameterCount()];
            Class<?>[] params = method.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                args[i] = arg(params[i], request);
            }
            method.setAccessible(true);
            ResponseEntity<?> response = (ResponseEntity<?>) method.invoke(handler, args);
            assertThat(response.getStatusCode().isError()).isTrue();
            assertThat(response.getBody()).isNotNull();
            handled++;
        }
        assertThat(handled).isGreaterThanOrEqualTo(5);
    }

    private static Object arg(Class<?> type, HttpServletRequest request) throws Exception {
        if (type == HttpServletRequest.class) return request;
        if (type == MethodArgumentNotValidException.class) {
            BindingResult result = mock(BindingResult.class);
            when(result.getFieldErrors()).thenReturn(List.of(new FieldError("request", "email", "must be valid")));
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(result);
            return ex;
        }
        if (Throwable.class.isAssignableFrom(type)) {
            if (type == Exception.class) return new Exception("boom");
            if (type == RuntimeException.class) return new RuntimeException("boom");
            if (type.getName().equals("org.springframework.web.servlet.resource.NoResourceFoundException")) {
                return type.getConstructor(HttpMethod.class, String.class).newInstance(HttpMethod.GET, "/missing");
            }
            for (Constructor<?> ctor : type.getDeclaredConstructors()) {
                ctor.setAccessible(true);
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 1 && params[0] == String.class) return ctor.newInstance("boom");
                if (params.length == 3) return ctor.newInstance("Thing", "id", 99);
            }
            return new Exception("boom");
        }
        return null;
    }
}
