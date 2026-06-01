package cl.tarrobuild.hardwareadvisor.dto;

import jakarta.validation.constraints.NotNull;

public record GenerateRecommendationsRequest(
        @NotNull(message = "Build ID cannot be null")
        Long buildId
) {}
