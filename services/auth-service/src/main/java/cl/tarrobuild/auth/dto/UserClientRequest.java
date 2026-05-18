package cl.tarrobuild.auth.dto;

public record UserClientRequest(
        String name,
        String lastName,
        String email,
        String phone
) {}
