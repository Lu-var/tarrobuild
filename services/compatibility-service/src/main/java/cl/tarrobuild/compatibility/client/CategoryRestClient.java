package cl.tarrobuild.compatibility.client;

import cl.tarrobuild.compatibility.dto.CategoryClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class CategoryRestClient {

    private final RestClient restClient;

    public CategoryRestClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("lb://category-service")
                .build();
    }

    /**
     * Resolves a category name by ID. Returns {@code null} on any communication failure
     * so callers can apply a fallback without breaking the compatibility check.
     */
    public CategoryClientResponse getCategoryById(Long id) {
        log.info("Calling category-service: GET /api/categories/{}", id);
        try {
            return restClient.get()
                    .uri("/api/categories/{id}", id)
                    .retrieve()
                    .body(CategoryClientResponse.class);
        } catch (RestClientException e) {
            log.warn("Could not resolve category name for ID {}: {}", id, e.getMessage());
            return null;
        }
    }
}
