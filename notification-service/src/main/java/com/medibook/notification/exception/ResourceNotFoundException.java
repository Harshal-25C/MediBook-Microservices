package com.medibook.notification.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private String resourceName;

    private String fieldName;

    private Object fieldValue;

    public ResourceNotFoundException(
            String resourceName,
            String fieldName,
            Object fieldValue) {

        // call parent constructor with formatted message
        super(String.format(
            "%s not found with %s: %s",
            resourceName,
            fieldName,
            fieldValue
        ));

        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    // getters for GlobalExceptionHandler to use
    public String getResourceName() { return resourceName; }
    public String getFieldName() { return fieldName; }
    public Object getFieldValue() { return fieldValue; }
}