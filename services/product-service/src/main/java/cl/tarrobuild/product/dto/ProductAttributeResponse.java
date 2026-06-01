package cl.tarrobuild.product.dto;

public record ProductAttributeResponse(
        Long id,
        String attributeName,
        String attributeValue,
        Long productId
) {}
