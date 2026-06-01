package cl.tarrobuild.notification.dto;

import cl.tarrobuild.notification.model.NotificationStatus;

import java.time.LocalDateTime;

public record NotificationLogResponse(
        Long id,
        Long userId,
        String type,
        String content,
        NotificationStatus status,
        LocalDateTime timestamp
) {}
