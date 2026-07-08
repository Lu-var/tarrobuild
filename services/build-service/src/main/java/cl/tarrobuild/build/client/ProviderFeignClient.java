package cl.tarrobuild.build.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "provider-service", url = "${provider-service.url}", fallbackFactory = ProviderFeignClientFallbackFactory.class)
public interface ProviderFeignClient {

    @GetMapping("/api/providers/{id}")
    String getProviderById(@PathVariable Long id);
}
