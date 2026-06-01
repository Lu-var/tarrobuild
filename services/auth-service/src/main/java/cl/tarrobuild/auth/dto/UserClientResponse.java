package cl.tarrobuild.auth.dto;

import java.time.LocalDateTime;

public record UserClientResponse(
        Long id,
        String name,
        String lastName,
        String email,
        String phone,
        LocalDateTime createdAt
) {}
