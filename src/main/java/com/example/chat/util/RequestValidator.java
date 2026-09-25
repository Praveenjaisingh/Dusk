package com.example.chat.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.chat.exception.ValidationException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Because functional endpoints (RouterFunction + ServerRequest/ServerResponse)
 * do NOT go through Spring MVC's @Valid argument-resolution pipeline, DTOs must
 * be validated manually. This helper runs Bean Validation (jakarta.validation)
 * against a DTO and throws a ValidationException (field -> message) on failure.
 */
@Component
public class RequestValidator {

    private final Validator validator;

    public RequestValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> void validate(T dto) {
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new LinkedHashMap<>();
            for (ConstraintViolation<T> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            throw new ValidationException(errors);
        }
    }
}
