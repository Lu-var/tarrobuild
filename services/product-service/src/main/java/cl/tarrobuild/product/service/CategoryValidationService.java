package cl.tarrobuild.product.service;

import cl.tarrobuild.product.client.CategoryRestClient;
import cl.tarrobuild.product.dto.CategoryClientResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class CategoryValidationService {

    private final CategoryRestClient categoryRestClient;

    public CategoryValidationService(CategoryRestClient categoryRestClient) {
        this.categoryRestClient = categoryRestClient;
    }

    public void validateCategoryExists(Long categoryId) {
        try {
            CategoryClientResponse response = categoryRestClient.getCategoryById(categoryId);
            log.info("Category validated: id={}, name={}, slug={}",
                    response.id(), response.name(), response.slug());
        } catch (RestClientException e) {
            log.warn("Category with id {} not found: {}", categoryId, e.getMessage());
            throw new EntityNotFoundException("Category with ID " + categoryId + " not found");
        }
    }
}
