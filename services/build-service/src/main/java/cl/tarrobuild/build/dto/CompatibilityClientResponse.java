package cl.tarrobuild.build.dto;

import java.time.LocalDateTime;

public record CompatibilityClientResponse(
        Long id,
        Long buildId,
        Boolean result,
        String details,
        LocalDateTime createdAt
) {}
