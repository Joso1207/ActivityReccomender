package org.chasapi.activityreccomender.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LocationNotFoundException.class)
    public ResponseEntity<String> handleLocationNotFound(
            LocationNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    @ExceptionHandler(ExternalServiceUnavailable.class)
    public ResponseEntity<String> handleExternalServiceUnavailable(
            ExternalServiceUnavailable ex) {

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("External service unavailable");
    }
}