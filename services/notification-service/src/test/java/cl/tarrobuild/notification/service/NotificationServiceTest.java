package cl.tarrobuild.notification.service;

import cl.tarrobuild.notification.client.UserRestClient;
import cl.tarrobuild.notification.dto.NotificationLogResponse;
import cl.tarrobuild.notification.dto.SendNotificationRequest;
import cl.tarrobuild.notification.dto.UserClientResponse;
import cl.tarrobuild.notification.model.NotificationLog;
import cl.tarrobuild.notification.model.NotificationStatus;
import cl.tarrobuild.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRestClient userRestClient;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<NotificationLog> logCaptor;

    private SendNotificationRequest request;
    private NotificationLog savedLog;
    private UserClientResponse userResponse;

    @BeforeEach
    void setUp() {
        request = new SendNotificationRequest(
                1L,
                "ORDER_CONFIRMATION",
                "Your order has been confirmed",
                NotificationStatus.SUCCESS
        );

        savedLog = new NotificationLog();
        savedLog.setId(100L);
        savedLog.setUserId(1L);
        savedLog.setType("ORDER_CONFIRMATION");
        savedLog.setContent("Your order has been confirmed");
        savedLog.setStatus(NotificationStatus.SUCCESS);
        savedLog.setTimestamp(LocalDateTime.of(2024, 1, 15, 10, 30, 0));

        userResponse = new UserClientResponse(
                1L,
                "Juan",
                "Pérez",
                "juan.perez@example.com",
                "+56912345678",
                LocalDateTime.of(2023, 6, 1, 0, 0, 0)
        );
    }

    @Test
    @DisplayName("send() - envía notificación exitosamente con resolución de usuario")
    void send_shouldSendSuccessfully_whenUserIsResolved() throws Exception {
        when(userRestClient.getUserById(1L)).thenReturn(userResponse);
        when(notificationRepository.save(any(NotificationLog.class))).thenReturn(savedLog);

        CompletableFuture<NotificationLogResponse> future = notificationService.send(request);
        NotificationLogResponse response = future.get();

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.type()).isEqualTo("ORDER_CONFIRMATION");
        assertThat(response.content()).isEqualTo("Your order has been confirmed");
        assertThat(response.status()).isEqualTo(NotificationStatus.SUCCESS);
        assertThat(response.timestamp()).isEqualTo(savedLog.getTimestamp());

        verify(userRestClient).getUserById(1L);
        verify(notificationRepository).save(logCaptor.capture());
        NotificationLog captured = logCaptor.getValue();
        assertThat(captured.getUserId()).isEqualTo(1L);
        assertThat(captured.getType()).isEqualTo("ORDER_CONFIRMATION");
        assertThat(captured.getContent()).isEqualTo("Your order has been confirmed");
        assertThat(captured.getStatus()).isEqualTo(NotificationStatus.SUCCESS);
    }

    @Test
    @DisplayName("send() - falla resolución de usuario pero la notificación se guarda igual")
    void send_shouldSaveNotification_whenUserResolutionFails() throws Exception {
        when(userRestClient.getUserById(1L)).thenThrow(new RuntimeException("Connection refused"));
        when(notificationRepository.save(any(NotificationLog.class))).thenReturn(savedLog);

        CompletableFuture<NotificationLogResponse> future = notificationService.send(request);
        NotificationLogResponse response = future.get();

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.type()).isEqualTo("ORDER_CONFIRMATION");

        verify(userRestClient).getUserById(1L);
        verify(notificationRepository).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("getAllLogs() - retorna todos los registros de notificación")
    void getAllLogs_shouldReturnAllLogs() {
        NotificationLog log2 = new NotificationLog();
        log2.setId(200L);
        log2.setUserId(2L);
        log2.setType("PAYMENT_RECEIVED");
        log2.setContent("Payment of $50 received");
        log2.setStatus(NotificationStatus.INFO);
        log2.setTimestamp(LocalDateTime.of(2024, 1, 15, 12, 0, 0));

        when(notificationRepository.findAll()).thenReturn(List.of(savedLog, log2));

        List<NotificationLogResponse> logs = notificationService.getAllLogs();

        assertThat(logs).hasSize(2);
        assertThat(logs.getFirst().id()).isEqualTo(100L);
        assertThat(logs.getFirst().userId()).isEqualTo(1L);
        assertThat(logs.get(1).id()).isEqualTo(200L);
        assertThat(logs.get(1).userId()).isEqualTo(2L);

        verify(notificationRepository).findAll();
    }

    @Test
    @DisplayName("getAllLogs() - retorna lista vacía cuando no hay registros")
    void getAllLogs_shouldReturnEmptyList_whenNoLogs() {
        when(notificationRepository.findAll()).thenReturn(List.of());

        List<NotificationLogResponse> logs = notificationService.getAllLogs();

        assertThat(logs).isEmpty();
        verify(notificationRepository).findAll();
    }

    @Test
    @DisplayName("getLogsByUserId() - retorna los registros de un usuario específico")
    void getLogsByUserId_shouldReturnLogsForUser() {
        when(notificationRepository.findByUserId(1L)).thenReturn(List.of(savedLog));

        List<NotificationLogResponse> logs = notificationService.getLogsByUserId(1L);

        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().id()).isEqualTo(100L);
        assertThat(logs.getFirst().userId()).isEqualTo(1L);

        verify(notificationRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("getLogsByUserId() - retorna lista vacía cuando el usuario no tiene registros")
    void getLogsByUserId_shouldReturnEmptyList_whenNoLogsForUser() {
        when(notificationRepository.findByUserId(99L)).thenReturn(List.of());

        List<NotificationLogResponse> logs = notificationService.getLogsByUserId(99L);

        assertThat(logs).isEmpty();
        verify(notificationRepository).findByUserId(99L);
    }

    @Test
    @DisplayName("getLogById() - retorna el registro cuando existe")
    void getLogById_shouldReturnLog_whenFound() {
        when(notificationRepository.findById(100L)).thenReturn(Optional.of(savedLog));

        NotificationLogResponse response = notificationService.getLogById(100L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.type()).isEqualTo("ORDER_CONFIRMATION");
        assertThat(response.content()).isEqualTo("Your order has been confirmed");
        assertThat(response.status()).isEqualTo(NotificationStatus.SUCCESS);
        assertThat(response.timestamp()).isEqualTo(savedLog.getTimestamp());

        verify(notificationRepository).findById(100L);
    }

    @Test
    @DisplayName("getLogById() - lanza EntityNotFoundException cuando no existe")
    void getLogById_shouldThrow_whenNotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getLogById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Notification log with ID 999 not found");

        verify(notificationRepository).findById(999L);
    }
}
