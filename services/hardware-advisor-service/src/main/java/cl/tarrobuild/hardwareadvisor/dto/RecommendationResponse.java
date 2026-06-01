package cl.tarrobuild.hardwareadvisor.dto;

import java.time.LocalDateTime;

public record RecommendationResponse(
        Long id,
        Long buildId,
        String ruleApplied,
        Long suggestedProductId,
        String reason,
        LocalDateTime createdAt
) {}
