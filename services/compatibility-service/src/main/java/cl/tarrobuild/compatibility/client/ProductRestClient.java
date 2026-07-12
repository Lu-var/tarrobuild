package cl.tarrobuild.compatibility.client;

import cl.tarrobuild.compatibility.dto.ProductClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class ProductRestClient {

    private final RestClient restClient;

    public ProductRestClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("lb://product-service")
                .build();
    }

    /**
     * Returns the product for the given ID, or {@code null} on any communication failure
     * so the compatibility check can skip unreachable products without aborting.
     */
    public ProductClientResponse getProductById(Long id) {
        log.info("Calling product-service: GET /api/products/{}", id);
        try {
            return restClient.get()
                    .uri("/api/products/{id}", id)
                    .retrieve()
                    .body(ProductClientResponse.class);
        } catch (RestClientException e) {
            log.warn("Could not fetch product ID {} from product-service: {}", id, e.getMessage());
            return null;
        }
    }
}
