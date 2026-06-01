package cl.tarrobuild.product.dto;

public record CategoryClientResponse(
        Long id,
        String name,
        String slug,
        String description,
        Boolean isActive
) {}
