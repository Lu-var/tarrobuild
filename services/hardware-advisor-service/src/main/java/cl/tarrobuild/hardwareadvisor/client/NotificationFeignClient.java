package cl.tarrobuild.hardwareadvisor.client;

import cl.tarrobuild.hardwareadvisor.dto.NotificationClientRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", fallbackFactory = NotificationFeignClientFallbackFactory.class)
public interface NotificationFeignClient {
    @PostMapping("/api/notifications/send")
    void sendNotification(@RequestBody NotificationClientRequest request);
}
