package cl.tarrobuild.build.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProviderFeignClientFallbackFactory implements FallbackFactory<ProviderFeignClient> {
    @Override
    public ProviderFeignClient create(Throwable cause) {
        log.error("provider-service is unavailable: {}", cause.getMessage());
        return id -> {
            log.warn("Returning null for provider ID {} due to provider-service outage", id);
            return null;
        };
    }
}
