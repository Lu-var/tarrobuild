package cl.tarrobuild.hardwareadvisor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BuildFeignClientFallbackFactory implements FallbackFactory<BuildFeignClient> {
    @Override
    public BuildFeignClient create(Throwable cause) {
        log.error("build-service is unavailable: {}", cause.getMessage());
        return id -> {
            log.warn("Returning null for build ID {} due to build-service outage", id);
            return null;
        };
    }
}
