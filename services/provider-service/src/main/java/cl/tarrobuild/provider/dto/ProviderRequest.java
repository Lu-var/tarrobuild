package cl.tarrobuild.provider.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProviderRequest(
        @NotBlank(message = "Name cannot be blank")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Size(max = 200, message = "Contact cannot exceed 200 characters")
        String contact,

        @Size(max = 500, message = "Website cannot exceed 500 characters")
        String website,

        Boolean isActive
) {}
