package cl.tarrobuild.provider.dto;

import jakarta.validation.constraints.NotNull;

public record ProviderProductRequest(
        @NotNull(message = "Product ID cannot be null")
        Long productId,

        String externalReference
) {}
