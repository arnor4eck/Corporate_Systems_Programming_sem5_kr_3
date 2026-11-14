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
    private ProblemDetail problem(HttpStatus status, String detail) {
        var attrs = (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
        return com.arnor4eck.pks_3.error.ApiProblems.create(status, detail, attrs.getRequest().getRequestURI());
    }
    @Override
    protected org.springframework.http.ResponseEntity<Object> createResponseEntity(Object body, org.springframework.http.HttpHeaders headers,
            org.springframework.http.HttpStatusCode status, org.springframework.web.context.request.WebRequest request) {
        if (body instanceof ProblemDetail p) {
            if (p.getType() == null) p.setType(java.net.URI.create("about:blank"));
            if (p.getTitle() == null) p.setTitle(org.springframework.http.HttpStatus.valueOf(status.value()).getReasonPhrase());
            p.setProperty("correlationId", com.arnor4eck.pks_3.filter.CorrelationIdFilter.current());
            if (p.getInstance() == null) p.setInstance(java.net.URI.create(((org.springframework.web.context.request.ServletWebRequest)request).getRequest().getRequestURI()));
        }
        return super.createResponseEntity(body, headers, status, request);
    }
    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class, jakarta.persistence.OptimisticLockException.class})
    public ProblemDetail stale(Exception exception) { return problem(HttpStatus.CONFLICT, "Record has changed; reload it and retry"); }
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail denied(Exception exception) {
        return problem(HttpStatus.FORBIDDEN, "Insufficient permissions");
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflict(DataIntegrityViolationException exception) {
        return problem(HttpStatus.CONFLICT,
                "Operation violates a unique constraint or a reference to another record");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception exception) {
        log.error("Unhandled API exception", exception);
        ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error");
        return problem;
    }
}
