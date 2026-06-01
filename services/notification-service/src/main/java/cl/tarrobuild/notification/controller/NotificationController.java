package cl.tarrobuild.notification.controller;

import cl.tarrobuild.notification.dto.NotificationLogResponse;
import cl.tarrobuild.notification.dto.SendNotificationRequest;
import cl.tarrobuild.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<NotificationLogResponse> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.send(request));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<NotificationLogResponse>> getAllLogs() {
        return ResponseEntity.ok(notificationService.getAllLogs());
    }

    @GetMapping("/logs/user/{userId}")
    public ResponseEntity<List<NotificationLogResponse>> getLogsByUserId(
            @PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getLogsByUserId(userId));
    }

    @GetMapping("/logs/{id}")
    public ResponseEntity<NotificationLogResponse> getLogById(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getLogById(id));
    }
}
