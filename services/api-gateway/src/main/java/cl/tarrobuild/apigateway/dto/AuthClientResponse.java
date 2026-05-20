package cl.tarrobuild.apigateway.dto;

public record AuthClientResponse(
        String token,
        String email,
        String role
) {}
