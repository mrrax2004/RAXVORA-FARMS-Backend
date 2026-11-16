package com.dairyfarm.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<?> notFound(NotFoundException e, HttpServletRequest r) {
        return response(HttpStatus.NOT_FOUND, e.getMessage(), r);
    }
    @ExceptionHandler(ConflictException.class)
    ResponseEntity<?> conflict(ConflictException e, HttpServletRequest r) {
        return response(HttpStatus.CONFLICT, e.getMessage(), r);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> badRequest(IllegalArgumentException e, HttpServletRequest r) {
        return response(HttpStatus.BAD_REQUEST, e.getMessage(), r);
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> uploadTooLarge(MaxUploadSizeExceededException e, HttpServletRequest r) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds the configured size limit.", r);
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<?> validation(Exception e, HttpServletRequest r) {
        String message = e instanceof MethodArgumentNotValidException m
                ? m.getBindingResult().getFieldErrors().stream().map(x -> x.getField() + ": " + x.getDefaultMessage())
                    .findFirst().orElse("Invalid request")
                : e.getMessage();
        return response(HttpStatus.BAD_REQUEST, message, r);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> dataConflict(DataIntegrityViolationException e, HttpServletRequest r) {
        return response(HttpStatus.CONFLICT, "A record with the same unique value already exists.", r);
    }
    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<?> unauthenticated(BadCredentialsException e, HttpServletRequest r) {
        return response(HttpStatus.UNAUTHORIZED, "Invalid username or password.", r);
    }
    private ResponseEntity<?> response(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now(), "status", status.value(),
                "error", status.getReasonPhrase(), "message", message, "path", request.getRequestURI()));
    }
}
