package cl.tarrobuild.estimate.dto;

import java.time.LocalDateTime;

public record EstimateResponse(
        Long id,
        Long buildId,
        Integer totalCost,
        String currency,
        LocalDateTime createdAt
) {}
