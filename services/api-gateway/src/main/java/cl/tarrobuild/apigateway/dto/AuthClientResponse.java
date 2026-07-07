package cl.tarrobuild.apigateway.dto;

public record AuthClientResponse(
        Long userId,
        String token,
        String email,
        String role
) {}
