package cl.tarrobuild.provider.dto;

public record ProviderResponse(
        Long id,
        String name,
        String contact,
        String website,
        Boolean isActive
) {}
