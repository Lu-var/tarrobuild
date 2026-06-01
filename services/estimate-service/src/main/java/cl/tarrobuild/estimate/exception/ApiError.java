package cl.tarrobuild.estimate.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String message,
        String details,
        String timestamp
) {}
