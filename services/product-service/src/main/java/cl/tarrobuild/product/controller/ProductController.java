package cl.tarrobuild.product.controller;

import cl.tarrobuild.product.dto.ProductAttributeRequest;
import cl.tarrobuild.product.dto.ProductAttributeResponse;
import cl.tarrobuild.product.dto.ProductRequest;
import cl.tarrobuild.product.dto.ProductResponse;
import cl.tarrobuild.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    @GetMapping("/brand/{brand}")
    public ResponseEntity<List<ProductResponse>> getProductsByBrand(@PathVariable String brand) {
        return ResponseEntity.ok(productService.getProductsByBrand(brand));
    }

    @GetMapping("/price")
    public ResponseEntity<List<ProductResponse>> getProductsByPriceRange(
            @RequestParam Integer minPrice,
            @RequestParam Integer maxPrice) {
        return ResponseEntity.ok(productService.getProductsByMsrpRange(minPrice, maxPrice));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id,
                                                          @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        boolean deleted = productService.deleteProduct(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateProduct(@PathVariable Long id) {
        productService.deactivateProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateProduct(@PathVariable Long id) {
        productService.activateProduct(id);
        return ResponseEntity.noContent().build();
    }

    // Product Attributes endpoints

    @GetMapping("/{id}/attributes")
    public ResponseEntity<List<ProductAttributeResponse>> getProductAttributes(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductAttributes(id));
    }

    @PostMapping("/{id}/attributes")
    public ResponseEntity<ProductAttributeResponse> addProductAttribute(
            @PathVariable Long id,
            @Valid @RequestBody ProductAttributeRequest request) {
        return new ResponseEntity<>(productService.addProductAttribute(id, request), HttpStatus.CREATED);
    }

    @PutMapping("/{productId}/attributes/{attributeId}")
    public ResponseEntity<ProductAttributeResponse> updateProductAttribute(
            @PathVariable Long productId,
            @PathVariable Long attributeId,
            @Valid @RequestBody ProductAttributeRequest request) {
        return ResponseEntity.ok(productService.updateProductAttribute(productId, attributeId, request));
    }

    @DeleteMapping("/{productId}/attributes/{attributeId}")
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