package cl.tarrobuild.build.client;

import cl.tarrobuild.build.dto.CompatibilityClientRequest;
import cl.tarrobuild.build.dto.CompatibilityClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "compatibility-service", url = "${compatibility-service.url}")
public interface CompatibilityFeignClient {

    @PostMapping("/api/compatibility/check")
    CompatibilityClientResponse checkCompatibility(@RequestBody CompatibilityClientRequest request);

    @GetMapping("/api/compatibility/check/{buildId}")
    CompatibilityClientResponse getLatestCheckByBuildId(@PathVariable Long buildId);
}
