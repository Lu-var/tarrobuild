package cl.tarrobuild.compatibility.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompatibilityRuleRequest(
        @NotBlank(message = "Source category cannot be blank")
        @Size(min = 2, max = 100, message = "Source category must be between 2 and 100 characters")
        String sourceCategory,

        @NotBlank(message = "Source attribute name cannot be blank")
        @Size(min = 2, max = 100, message = "Source attribute name must be between 2 and 100 characters")
        String sourceAttributeName,

        @NotBlank(message = "Operator cannot be blank")
        @Size(min = 2, max = 20, message = "Operator must be between 2 and 20 characters")
        String operator,

        @NotBlank(message = "Target category cannot be blank")
        @Size(min = 2, max = 100, message = "Target category must be between 2 and 100 characters")
        String targetCategory,

        @NotBlank(message = "Target attribute name cannot be blank")
        @Size(min = 2, max = 100, message = "Target attribute name must be between 2 and 100 characters")
        String targetAttributeName,

        @NotBlank(message = "Incompatibility reason cannot be blank")
        @Size(min = 5, max = 500, message = "Incompatibility reason must be between 5 and 500 characters")
        String incompatibilityReason
) {}
