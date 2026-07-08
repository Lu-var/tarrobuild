package cl.tarrobuild.hardwareadvisor.dto;

public record ProductAttributeClientResponse(
        Long id,
        Long productId,
        String attributeName,
        String attributeValue
) {}
