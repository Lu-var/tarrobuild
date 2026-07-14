package cl.tarrobuild.build.client;

import cl.tarrobuild.build.dto.ProductClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", fallbackFactory = ProductFeignClientFallbackFactory.class)
public interface ProductFeignClient {
    @GetMapping("/api/products/{id}")
    ProductClientResponse getProductById(@PathVariable Long id);
}
