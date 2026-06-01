package cl.tarrobuild.build.client;

import cl.tarrobuild.build.dto.ProviderClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "provider-service", url = "${provider-service.url}")
public interface ProviderFeignClient {

    @GetMapping("/api/providers/{providerId}/products")
    List<ProviderClientResponse> getProductsByProvider(@PathVariable Long providerId);
}
