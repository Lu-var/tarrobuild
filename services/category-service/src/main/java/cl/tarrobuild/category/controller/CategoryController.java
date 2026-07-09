package cl.tarrobuild.category.controller;

import cl.tarrobuild.category.dto.AttributeDefinitionRequest;
import cl.tarrobuild.category.dto.AttributeDefinitionResponse;
import cl.tarrobuild.category.dto.CategoryRequest;
import cl.tarrobuild.category.dto.CategoryResponse;
import cl.tarrobuild.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Gestión de categorías de componentes")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @Operation(summary = "Crear una nueva categoría", description = "Crea una categoría de componente con nombre y descripción")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return new ResponseEntity<>(categoryService.createCategory(request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Obtener todas las categorías", description = "Retorna el listado completo de categorías de componentes disponibles")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener categoría por ID", description = "Retorna una categoría específica según su identificador único")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Actualizar parcialmente una categoría", description = "Actualiza los campos de una categoría existente identificada por su ID")
    public ResponseEntity<CategoryResponse> patchCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.patchCategory(id, request));
    }

    @PostMapping("/{categoryId}/attributes")
    @Operation(summary = "Crear atributo en una categoría", description = "Agrega un nuevo atributo a la categoría especificada")
    public ResponseEntity<AttributeDefinitionResponse> createAttribute(
            @PathVariable Long categoryId,
            @Valid @RequestBody AttributeDefinitionRequest request) {
        return new ResponseEntity<>(
                categoryService.createAttribute(categoryId, request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{categoryId}/attributes")
    @Operation(summary = "Obtener atributos por categoría", description = "Retorna todos los atributos definidos para una categoría específica")
    public ResponseEntity<List<AttributeDefinitionResponse>> getAttributesByCategory(
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(categoryService.getAttributesByCategory(categoryId));
    }
}