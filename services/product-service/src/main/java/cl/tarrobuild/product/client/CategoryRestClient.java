package cl.tarrobuild.product.client;

import cl.tarrobuild.product.dto.CategoryClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class CategoryRestClient {

    private final RestClient restClient;

    public CategoryRestClient(RestClient.Builder builder,
                               @Value("${category-service.url}") String categoryServiceUrl) {
        this.restClient = builder
                .baseUrl(categoryServiceUrl)
                .build();
    }

    public CategoryClientResponse getCategoryById(Long id) {
        log.info("Calling category-service: GET /api/categories/{}", id);
        CategoryClientResponse response = restClient.get()
                .uri("/api/categories/{id}", id)
                .retrieve()
                .body(CategoryClientResponse.class);
        log.info("Category-service response: id={}, name={}", response.id(), response.name());
        return response;
    }
}
