package cl.tarrobuild.compatibility.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompatibilityCheckRequest(
        @NotNull(message = "Build ID cannot be null")
        Long buildId,

        @NotNull(message = "Product IDs cannot be null")
        @Size(min = 2, message = "At least 2 products are required for a compatibility check")
        List<Long> productIds
) {}
