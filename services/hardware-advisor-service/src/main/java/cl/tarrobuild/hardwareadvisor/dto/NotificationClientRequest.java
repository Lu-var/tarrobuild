package cl.tarrobuild.hardwareadvisor.dto;

public record NotificationClientRequest(
        Long userId,
        String type,
        String content,
        String status
) {}
