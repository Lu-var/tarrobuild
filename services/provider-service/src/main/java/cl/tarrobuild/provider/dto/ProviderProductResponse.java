package cl.tarrobuild.provider.dto;

public record ProviderProductResponse(
        Long id,
        Long providerId,
        Long productId,
        String externalReference
) {}
