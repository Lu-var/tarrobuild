package cl.tarrobuild.hardwareadvisor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationFeignClientFallbackFactory implements FallbackFactory<NotificationFeignClient> {
    @Override
    public NotificationFeignClient create(Throwable cause) {
        log.error("notification-service is unavailable: {}", cause.getMessage());
        return request -> {
            log.warn("Skipping notification of type '{}' for user {} due to service outage",
                    request.type(), request.userId());
        };
    }
}
