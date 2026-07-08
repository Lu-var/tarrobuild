package cl.tarrobuild.hardwareadvisor.client;

import cl.tarrobuild.hardwareadvisor.dto.ProductClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "product-service", url = "${product-service.url}", fallbackFactory = ProductFeignClientFallbackFactory.class)
public interface ProductFeignClient {
    @GetMapping("/api/products/{id}")
    ProductClientResponse getProductById(@PathVariable Long id);

    @GetMapping("/api/products/category/{categoryId}")
    List<ProductClientResponse> getProductsByCategory(@PathVariable Long categoryId);
}
