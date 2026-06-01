package cl.tarrobuild.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductAttributeRequest(
        @NotBlank(message = "Attribute name cannot be blank")
        @Size(min = 2, max = 100, message = "Attribute name must be between 2 and 100 characters")
        String attributeName,

        @NotBlank(message = "Attribute value cannot be blank")
        @Size(max = 255, message = "Attribute value cannot exceed 255 characters")
        String attributeValue
) {}
