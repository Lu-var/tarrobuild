package cl.tarrobuild.notification.service;

import cl.tarrobuild.notification.client.UserRestClient;
import cl.tarrobuild.notification.dto.NotificationLogResponse;
import cl.tarrobuild.notification.dto.SendNotificationRequest;
import cl.tarrobuild.notification.dto.UserClientResponse;
import cl.tarrobuild.notification.model.NotificationLog;
import cl.tarrobuild.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRestClient userRestClient;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRestClient userRestClient) {
        this.notificationRepository = notificationRepository;
        this.userRestClient = userRestClient;
    }

    @Async("notificationExecutor")
    public CompletableFuture<NotificationLogResponse> send(SendNotificationRequest request) {
        log.info("Processing notification to userId: {} type: {}", request.userId(), request.type());
        try {
            UserClientResponse user = userRestClient.getUserById(request.userId());
            log.info("Resolved user: {} {} <{}>", user.name(), user.lastName(), user.email());
        } catch (Exception e) {
            log.warn("Could not resolve user {}: {}", request.userId(), e.getMessage());
        }

        NotificationLog record = new NotificationLog();
        record.setUserId(request.userId());
        record.setType(request.type());
        record.setContent(request.content());
        record.setStatus(request.status());
        record.setTimestamp(LocalDateTime.now());

        NotificationLog saved = notificationRepository.save(record);
        log.info("Notification sent with id: {}", saved.getId());
        return CompletableFuture.completedFuture(toResponse(saved));
    }

    public List<NotificationLogResponse> getAllLogs() {
        log.info("Getting all notification logs");
        return notificationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<NotificationLogResponse> getLogsByUserId(Long userId) {
        log.info("Getting notification logs for userId: {}", userId);
        return notificationRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public NotificationLogResponse getLogById(Long id) {
        log.info("Getting notification log by id: {}", id);
        return notificationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Notification log with ID " + id + " not found"));
    }

    private NotificationLogResponse toResponse(NotificationLog record) {
        return new NotificationLogResponse(
                record.getId(),
                record.getUserId(),
                record.getType(),
                record.getContent(),
                record.getStatus(),
                record.getTimestamp()
        );
    }
}
