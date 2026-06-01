package cl.tarrobuild.category.service;

import cl.tarrobuild.category.dto.AttributeDefinitionRequest;
import cl.tarrobuild.category.dto.AttributeDefinitionResponse;
import cl.tarrobuild.category.dto.CategoryRequest;
import cl.tarrobuild.category.dto.CategoryResponse;
import cl.tarrobuild.category.model.AttributeDefinition;
import cl.tarrobuild.category.model.Category;
import cl.tarrobuild.category.repository.AttributeDefinitionRepository;
import cl.tarrobuild.category.repository.CategoryRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AttributeDefinitionRepository attributeDefinitionRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           AttributeDefinitionRepository attributeDefinitionRepository) {
        this.categoryRepository = categoryRepository;
        this.attributeDefinitionRepository = attributeDefinitionRepository;
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating category \"{}\" slug: {}", request.name(), request.slug());
        if (categoryRepository.findBySlug(request.slug()).isPresent()) {
            log.warn("Slug \"{}\" already exists", request.slug());
            throw new EntityExistsException("Slug already exists");
        }

        Category newCategory = new Category();
        newCategory.setName(request.name());
        newCategory.setSlug(request.slug());
        newCategory.setDescription(request.description());

        Category saved = categoryRepository.save(newCategory);
        log.info("Category created with id: {}", saved.getId());
        return toResponse(saved);
    }

    public List<CategoryResponse> getAllCategories() {
        log.info("Getting all categories");
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse getCategoryById(Long id) {
        log.info("Getting category by id: {}", id);
        return categoryRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
    }

    public AttributeDefinitionResponse createAttribute(Long categoryId, AttributeDefinitionRequest request) {
        log.info("Creating attribute \"{}\" for category id: {}", request.attributeName(), categoryId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        if (attributeDefinitionRepository.existsByAttributeNameAndCategoryId(request.attributeName(), categoryId)) {
            log.warn("Attribute \"{}\" already exists for category {}", request.attributeName(), categoryId);
            throw new EntityExistsException("Attribute with name '" + request.attributeName() + "' already exists");
        }

        AttributeDefinition newDefinition = new AttributeDefinition();
        newDefinition.setAttributeName(request.attributeName());
        newDefinition.setValueType(request.valueType());
        newDefinition.setIsRequired(request.isRequired());
        newDefinition.setCategory(category);

        AttributeDefinition saved = attributeDefinitionRepository.save(newDefinition);
        log.info("Attribute created with id: {} for category id: {}", saved.getId(), categoryId);
        return toAttributeResponse(saved);
    }

    public List<AttributeDefinitionResponse> getAttributesByCategory(Long categoryId) {
        log.info("Getting attributes for category id: {}", categoryId);
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        return attributeDefinitionRepository.findByCategoryId(categoryId)
                .stream()
                .map(this::toAttributeResponse)
                .toList();
    }

    private AttributeDefinitionResponse toAttributeResponse(AttributeDefinition attributeDefinition) {
        return new AttributeDefinitionResponse(
                attributeDefinition.getId(),
                attributeDefinition.getAttributeName(),
                attributeDefinition.getValueType(),
                attributeDefinition.getIsRequired(),
                attributeDefinition.getCategory().getId()
        );
    }

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getIsActive(),
                category.getAttributes()
                        .stream()
                        .map(this::toAttributeResponse)
                        .toList()
        );
    }
}
