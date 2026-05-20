package cl.tarrobuild.estimate.exception;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Value("${app.environment:production}")
    private String environment;

    private boolean isDevelopment() {
        return "development".equalsIgnoreCase(environment);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());

        String details = isDevelopment() ? Arrays.toString(e.getStackTrace()) : null;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(e.getMessage(), details, LocalDateTime.now().toString()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid arguments");
        log.warn("Invalid arguments: {}", message);

        String details = isDevelopment() ? Arrays.toString(e.getStackTrace()) : null;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(message, details, LocalDateTime.now().toString()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleNotFound(EntityNotFoundException e) {
        log.warn("Element not found: {}", e.getMessage());

        String details = isDevelopment() ? Arrays.toString(e.getStackTrace()) : null;
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(e.getMessage(), details, LocalDateTime.now().toString()));
    }

    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<?> handleConflict(EntityExistsException e) {
        log.warn("Conflict: {}", e.getMessage());

        String details = isDevelopment() ? Arrays.toString(e.getStackTrace()) : null;
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(e.getMessage(), details, LocalDateTime.now().toString()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());

        String details = isDevelopment() ? Arrays.toString(e.getStackTrace()) : null;
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(e.getMessage(), details, LocalDateTime.now().toString()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Invalid request body: {}", e.getMessage());
        String details = isDevelopment() ? Arrays.toString(e.getStackTrace()) : null;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Invalid request body", details, LocalDateTime.now().toString()));
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<?> handleNumberFormat(NumberFormatException e) {
        log.warn("Invalid number format: {}", e.getMessage());

        String details = isDevelopment() ? Arrays.toString(e.getStackTrace()) : null;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Invalid number format: " + e.getMessage(), details, LocalDateTime.now().toString()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());

        String details = isDevelopment() ? Arrays.toString(e.getStackTrace()) : null;
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("Access denied", details, LocalDateTime.now().toString()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception e) {
        log.error("Uncaptured exception", e);

        String details = isDevelopment() ? Arrays.toString(e.getStackTrace()) : null;
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("Internal server error", details, LocalDateTime.now().toString()));
    }
}
