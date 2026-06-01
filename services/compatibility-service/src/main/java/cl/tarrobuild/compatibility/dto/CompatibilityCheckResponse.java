package cl.tarrobuild.compatibility.dto;

import java.time.LocalDateTime;

public record CompatibilityCheckResponse(
        Long id,
        Long buildId,
        Boolean result,
        String details,
        LocalDateTime createdAt
) {}
