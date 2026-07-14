package cl.tarrobuild.hardwareadvisor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CompatibilityFeignClientFallbackFactory implements FallbackFactory<CompatibilityFeignClient> {
    @Override
    public CompatibilityFeignClient create(Throwable cause) {
        log.error("compatibility-service is unavailable: {}", cause.getMessage());
        return request -> {
            log.warn("Skipping compatibility check for build ID {} due to service outage", request.buildId());
            return null;
        };
    }
}
