package cl.tarrobuild.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @NotNull(message = "MSRP cannot be null")
        @Min(value = 0, message = "MSRP must be equal or greater than 0")
        Integer msrp,

        @NotNull(message = "Category ID cannot be null")
        Long categoryId,

        @NotBlank(message = "Brand cannot be blank")
        @Size(min = 2, max = 100, message = "Brand must be between 2 and 100 characters")
        String brand,

        @NotBlank(message = "Model cannot be blank")
        @Size(min = 2, max = 100, message = "Model must be between 2 and 100 characters")
        String model,

        Boolean isActive
) {}
