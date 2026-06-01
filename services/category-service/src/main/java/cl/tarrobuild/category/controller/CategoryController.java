package cl.tarrobuild.category.controller;

import cl.tarrobuild.category.dto.AttributeDefinitionRequest;
import cl.tarrobuild.category.dto.AttributeDefinitionResponse;
import cl.tarrobuild.category.dto.CategoryRequest;
import cl.tarrobuild.category.dto.CategoryResponse;
import cl.tarrobuild.category.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return new ResponseEntity<>(categoryService.createCategory(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PostMapping("/{categoryId}/attributes")
    public ResponseEntity<AttributeDefinitionResponse> createAttribute(
            @PathVariable Long categoryId,
            @Valid @RequestBody AttributeDefinitionRequest request) {
        return new ResponseEntity<>(
                categoryService.createAttribute(categoryId, request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{categoryId}/attributes")
    public ResponseEntity<List<AttributeDefinitionResponse>> getAttributesByCategory(
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(categoryService.getAttributesByCategory(categoryId));
    }
}
