package cl.tarrobuild.notification.controller;

import cl.tarrobuild.notification.dto.NotificationLogResponse;
import cl.tarrobuild.notification.dto.SendNotificationRequest;
import cl.tarrobuild.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Envío y consulta de notificaciones del sistema")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    @Operation(summary = "Enviar notificación", description = "Envía una notificación a un usuario del sistema")
    public ResponseEntity<Void> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {
        notificationService.send(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/logs")
    @Operation(summary = "Listar todos los logs de notificaciones", description = "Retorna el registro completo de todas las notificaciones enviadas")
    public ResponseEntity<List<NotificationLogResponse>> getAllLogs() {
        return ResponseEntity.ok(notificationService.getAllLogs());
    }

    @GetMapping("/logs/user/{userId}")
    @Operation(summary = "Obtener logs por usuario", description = "Retorna las notificaciones enviadas a un usuario específico")
    public ResponseEntity<List<NotificationLogResponse>> getLogsByUserId(
            @PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getLogsByUserId(userId));
    }

    @GetMapping("/logs/{id}")
    @Operation(summary = "Obtener log por ID", description = "Retorna un registro de notificación específico según su identificador")
    public ResponseEntity<NotificationLogResponse> getLogById(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getLogById(id));
    }
}
