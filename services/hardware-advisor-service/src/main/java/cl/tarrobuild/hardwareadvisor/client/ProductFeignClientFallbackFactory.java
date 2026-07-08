package cl.tarrobuild.hardwareadvisor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductFeignClientFallbackFactory implements FallbackFactory<ProductFeignClient> {
    @Override
    public ProductFeignClient create(Throwable cause) {
        log.error("product-service is unavailable: {}", cause.getMessage());
        return new ProductFeignClient() {
            @Override
            public ProductClientResponse getProductById(Long id) {
                log.warn("Returning null for product ID {} due to product-service outage", id);
                return null;
            }

            @Override
            public List<ProductClientResponse> getProductsByCategory(Long categoryId) {
                log.warn("Returning empty list for category {} due to product-service outage", categoryId);
                return List.of();
            }
        };
    }
}
