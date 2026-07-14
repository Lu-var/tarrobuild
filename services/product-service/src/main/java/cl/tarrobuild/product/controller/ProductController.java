package cl.tarrobuild.product.controller;

import cl.tarrobuild.product.dto.ProductAttributeRequest;
import cl.tarrobuild.product.dto.ProductAttributeResponse;
import cl.tarrobuild.product.dto.ProductRequest;
import cl.tarrobuild.product.dto.ProductResponse;
import cl.tarrobuild.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Catálogo de componentes de hardware")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Obtener todos los productos", description = "Retorna el catálogo completo de componentes de hardware disponibles")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID", description = "Retorna un producto específico según su identificador único. Retorna 404 si no existe.")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Obtener productos por categoría", description = "Retorna todos los productos que pertenecen a una categoría específica")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    @GetMapping("/brand/{brand}")
    @Operation(summary = "Obtener productos por marca", description = "Retorna todos los productos filtrados por una marca específica")
    public ResponseEntity<List<ProductResponse>> getProductsByBrand(@PathVariable String brand) {
        return ResponseEntity.ok(productService.getProductsByBrand(brand));
    }

    @GetMapping("/price")
    @Operation(summary = "Obtener productos por rango de precio", description = "Retorna productos filtrados por un rango de precio mínimo y máximo")
    public ResponseEntity<List<ProductResponse>> getProductsByPriceRange(
            @RequestParam Integer minPrice,
            @RequestParam Integer maxPrice) {
        return ResponseEntity.ok(productService.getProductsByMsrpRange(minPrice, maxPrice));
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo producto", description = "Agrega un nuevo componente de hardware al catálogo")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un producto", description = "Reemplaza completamente los datos de un producto existente. Retorna 404 si no existe.")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id,
                                                           @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un producto", description = "Elimina un producto del catálogo. Retorna 404 si no existe.")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        boolean deleted = productService.deleteProduct(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desactivar un producto", description = "Marca un producto como inactivo. Retorna 404 si no existe.")
    public ResponseEntity<Void> deactivateProduct(@PathVariable Long id) {
        productService.deactivateProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activar un producto", description = "Marca un producto inactivo como activo nuevamente. Retorna 404 si no existe.")
    public ResponseEntity<Void> activateProduct(@PathVariable Long id) {
        productService.activateProduct(id);
        return ResponseEntity.noContent().build();
    }

    // Product Attributes endpoints

    @GetMapping("/{id}/attributes")
    @Operation(summary = "Obtener atributos de un producto", description = "Retorna todos los atributos asociados a un producto específico")
    public ResponseEntity<List<ProductAttributeResponse>> getProductAttributes(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductAttributes(id));
    }

    @PostMapping("/{id}/attributes")
    @Operation(summary = "Agregar atributo a un producto", description = "Asocia un nuevo atributo a un producto específico")
    public ResponseEntity<ProductAttributeResponse> addProductAttribute(
            @PathVariable Long id,
            @Valid @RequestBody ProductAttributeRequest request) {
        return new ResponseEntity<>(productService.addProductAttribute(id, request), HttpStatus.CREATED);
    }

    @PutMapping("/{productId}/attributes/{attributeId}")
    @Operation(summary = "Actualizar atributo de un producto", description = "Actualiza el valor de un atributo específico asociado a un producto. Retorna 404 si no existe.")
    public ResponseEntity<ProductAttributeResponse> updateProductAttribute(
            @PathVariable Long productId,
            @PathVariable Long attributeId,
            @Valid @RequestBody ProductAttributeRequest request) {
        return ResponseEntity.ok(productService.updateProductAttribute(productId, attributeId, request));
    }

    @DeleteMapping("/{productId}/attributes/{attributeId}")
    @Operation(summary = "Eliminar atributo de un producto", description = "Elimina un atributo específico asociado a un producto. Retorna 404 si no existe.")
    public ResponseEntity<Void> deleteProductAttribute(
            @PathVariable Long productId,
            @PathVariable Long attributeId) {
        boolean deleted = productService.deleteProductAttribute(productId, attributeId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}