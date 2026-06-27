package cl.tarrobuild.notification.service;

import cl.tarrobuild.notification.client.UserRestClient; // Asegúrate de ajustar este import a tu cliente real
import cl.tarrobuild.notification.dto.NotificationLogResponse;
import cl.tarrobuild.notification.dto.SendNotificationRequest;
import cl.tarrobuild.notification.model.NotificationLog;
import cl.tarrobuild.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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

    public NotificationLogResponse send(SendNotificationRequest request) {
        log.info("Sending notification to userId: {} type: {}", request.userId(), request.type());

        // Resolviendo el email a través del cliente Feign / RestClient antes de proceder
        try {
            var userResponse = userRestClient.getUserById(request.userId());
            log.info("Resolved target email: {} for notification", userResponse.email());
        } catch (Exception e) {
            log.error("Could not resolve email for userId: {}. Proceeding anyway.", request.userId());
        }

        NotificationLog record = new NotificationLog();
        record.setUserId(request.userId());
        record.setType(request.type());
        record.setContent(request.content());
        record.setStatus(request.status());
        record.setTimestamp(LocalDateTime.now());

        NotificationLog saved = notificationRepository.save(record);
        log.info("Notification sent with id: {}", saved.getId());
        return toResponse(saved);
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