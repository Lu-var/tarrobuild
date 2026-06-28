package cl.tarrobuild.compatibility.dto;

public record ProductAttributeClientResponse(
        Long id,
        String attributeName,
        String attributeValue,
        Long productId
) {}
