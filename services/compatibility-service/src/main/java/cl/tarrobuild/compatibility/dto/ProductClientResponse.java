package cl.tarrobuild.compatibility.dto;

import java.util.List;

public record ProductClientResponse(
        Long id,
        String name,
        String description,
        int price,
        Long categoryId,
        String brand,
        String model,
        boolean isActive,
        List<ProductAttributeClientResponse> attributes
) {}
