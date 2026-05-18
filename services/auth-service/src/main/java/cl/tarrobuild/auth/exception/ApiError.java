package cl.tarrobuild.auth.exception;

public record ApiError(
        String message,
        String details,
        String timestamp
) {}
