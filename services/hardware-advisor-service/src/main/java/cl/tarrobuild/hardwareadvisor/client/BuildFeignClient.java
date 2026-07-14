package cl.tarrobuild.hardwareadvisor.client;

import cl.tarrobuild.hardwareadvisor.dto.BuildClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "build-service", fallbackFactory = BuildFeignClientFallbackFactory.class)
public interface BuildFeignClient {
    @GetMapping("/api/builds/{id}")
    BuildClientResponse getBuildById(@PathVariable Long id);
}
