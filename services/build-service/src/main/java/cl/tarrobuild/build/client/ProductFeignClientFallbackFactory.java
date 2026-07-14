package cl.tarrobuild.build.client;

import cl.tarrobuild.build.dto.ProductClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductFeignClientFallbackFactory implements FallbackFactory<ProductFeignClient> {
    @Override
    public ProductFeignClient create(Throwable cause) {
        log.error("product-service is unavailable: {}", cause.getMessage());
        return id -> {
            log.warn("Returning null for product ID {} due to product-service outage", id);
            return null;
        };
    }
}
