package cl.tarrobuild.compatibility.dto;

import java.util.List;

public record ProductAttributeClientResponse(
        Long id,
        String attributeName,
        String attributeValue,
        Long productId
) {}
