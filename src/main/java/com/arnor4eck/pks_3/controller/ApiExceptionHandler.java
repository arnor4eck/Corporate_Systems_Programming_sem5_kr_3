package com.arnor4eck.pks_3.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflict(DataIntegrityViolationException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Operation violates a unique constraint or a reference to another record");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception exception) {
        log.error("Unhandled API exception", exception);
        String message = exception.getMessage();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                message == null || message.isBlank() ? "Unexpected server error" : message);
        problem.setProperty("exception", exception.getClass().getSimpleName());
        return problem;
    }
}
