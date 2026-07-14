package cl.tarrobuild.build.dto;

public record NotificationClientRequest(
    Long userId,
    String type,
    String content,
    String status
) {}
