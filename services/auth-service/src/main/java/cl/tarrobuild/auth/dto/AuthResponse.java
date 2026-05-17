package cl.tarrobuild.auth.dto;

public record AuthResponse(
        String token,
        String email,
        String role
) {}
