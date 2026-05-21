package cl.tarrobuild.estimate.client;

import cl.tarrobuild.estimate.dto.ProductClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class ProductRestClient {

    private final RestClient restClient;

    public ProductRestClient(RestClient.Builder builder,
                             @Value("${product-service.url}") String productServiceUrl) {
        this.restClient = builder
                .baseUrl(productServiceUrl)
                .build();
    }

    public ProductClientResponse getProductById(Long id) {
        log.info("Calling product-service: GET /api/products/{}", id);
        return restClient.get()
                .uri("/api/products/{id}", id)
                .retrieve()
                .body(ProductClientResponse.class);
    }
}
