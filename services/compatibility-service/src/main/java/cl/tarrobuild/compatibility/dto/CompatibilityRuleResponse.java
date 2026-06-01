package cl.tarrobuild.compatibility.dto;

public record CompatibilityRuleResponse(
        Long id,
        String sourceCategory,
        String sourceAttributeName,
        String operator,
        String targetCategory,
        String targetAttributeName,
        String incompatibilityReason
) {}
