package cl.tarrobuild.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        //Email format assumed valid from registration
        @NotBlank(message = "Email cannot be blank")
        String email,

        //Password size assumed valid from registration
        @NotBlank(message = "Password cannot be blank")
        String password
) {}
