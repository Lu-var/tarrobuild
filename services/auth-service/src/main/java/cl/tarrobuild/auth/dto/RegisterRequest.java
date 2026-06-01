package cl.tarrobuild.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email cannot be empty")
        String email,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 6, message = "Password must between 6 and 30 characters")
        String password,

        @NotBlank(message = "Name cannot be empty")
        @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Last name cannot be empty")
        @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
        String lastName,

        @NotBlank
        @Size(min = 8, max = 20, message = "Phone must be between 8 and 20 characters")
        String phone
) {}
