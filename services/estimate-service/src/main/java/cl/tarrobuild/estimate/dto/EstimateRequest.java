package cl.tarrobuild.estimate.dto;

import jakarta.validation.constraints.NotNull;

public record EstimateRequest(
        @NotNull(message = "Build ID cannot be null")
        Long buildId,

        String currency
) {}
