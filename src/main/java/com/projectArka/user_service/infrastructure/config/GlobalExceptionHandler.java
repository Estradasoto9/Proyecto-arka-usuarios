package com.projectArka.user_service.infrastructure.config;

import com.projectArka.user_service.domain.exception.UserAlreadyExistsException;
import com.projectArka.user_service.domain.exception.UserNotFoundException;
import com.projectArka.user_service.domain.exception.InvalidCredentialsException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Mono<Map<String, String>> createErrorResponse(String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", message);
        return Mono.just(errorResponse);
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Map<String, String>> handleUserNotFoundException(UserNotFoundException ex) {
        return createErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Map<String, String>> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        return createErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Mono<Map<String, String>> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        return createErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<Map<String, String>> handleAllExceptions(Throwable ex) {
        System.err.println("An unexpected error occurred: " + ex.getMessage());
        return createErrorResponse("An unexpected error occurred: " + ex.getMessage());
    }
}