package cl.tarrobuild.estimate.client;

import cl.tarrobuild.estimate.dto.NotificationClientRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class NotificationRestClient {

    private final RestClient restClient;

    public NotificationRestClient(RestClient.Builder builder,
                                  @Value("${notification-service.url}") String notificationServiceUrl) {
        this.restClient = builder
                .baseUrl(notificationServiceUrl)
                .build();
    }

    public void sendNotification(NotificationClientRequest request) {
        try {
            log.info("Calling notification-service: POST /api/notifications/send");
            restClient.post()
                    .uri("/api/notifications/send")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Notification sent successfully");
        } catch (Exception e) {
            log.warn("Failed to send notification: {}", e.getMessage());
        }
    }
}
