package cl.tarrobuild.build.dto;

public record ProviderClientResponse(
        Long id,
        Long providerId,
        Long productId
) {}
