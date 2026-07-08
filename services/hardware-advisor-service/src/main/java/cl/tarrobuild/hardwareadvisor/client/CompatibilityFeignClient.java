package cl.tarrobuild.hardwareadvisor.client;

import cl.tarrobuild.hardwareadvisor.dto.CompatibilityCheckRequest;
import cl.tarrobuild.hardwareadvisor.dto.CompatibilityCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "compatibility-service", url = "${compatibility-service.url}", fallbackFactory = CompatibilityFeignClientFallbackFactory.class)
public interface CompatibilityFeignClient {
    @PostMapping("/api/compatibility/check")
    CompatibilityCheckResponse checkCompatibility(@RequestBody CompatibilityCheckRequest request);
}
