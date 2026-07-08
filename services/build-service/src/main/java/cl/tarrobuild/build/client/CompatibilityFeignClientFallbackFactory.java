package cl.tarrobuild.build.client;

import cl.tarrobuild.build.dto.CompatibilityClientRequest;
import cl.tarrobuild.build.dto.CompatibilityClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CompatibilityFeignClientFallbackFactory implements FallbackFactory<CompatibilityFeignClient> {
    @Override
    public CompatibilityFeignClient create(Throwable cause) {
        log.error("compatibility-service is unavailable: {}", cause.getMessage());
        return new CompatibilityFeignClient() {
            @Override
            public CompatibilityClientResponse checkCompatibility(CompatibilityClientRequest request) {
                log.warn("Skipping compatibility check for build {} due to service outage", request.buildId());
                return null;
            }

            @Override
            public CompatibilityClientResponse getLatestCheckByBuildId(Long buildId) {
                log.warn("Skipping compatibility lookup for build {} due to service outage", buildId);
                return null;
            }
        };
    }
}
