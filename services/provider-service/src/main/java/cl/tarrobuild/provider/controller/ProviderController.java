package cl.tarrobuild.provider.controller;

import cl.tarrobuild.provider.dto.ProviderProductRequest;
import cl.tarrobuild.provider.dto.ProviderProductResponse;
import cl.tarrobuild.provider.dto.ProviderRequest;
import cl.tarrobuild.provider.dto.ProviderResponse;
import cl.tarrobuild.provider.service.ProviderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @PostMapping
    public ResponseEntity<ProviderResponse> createProvider(
            @Valid @RequestBody ProviderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerService.createProvider(request));
    }

    @GetMapping
    public ResponseEntity<List<ProviderResponse>> getAllProviders() {
        return ResponseEntity.ok(providerService.getAllProviders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProviderResponse> getProviderById(@PathVariable Long id) {
        return ResponseEntity.ok(providerService.getProviderById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProviderResponse> updateProvider(
            @PathVariable Long id,
            @Valid @RequestBody ProviderRequest request) {
        return ResponseEntity.ok(providerService.updateProvider(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProvider(@PathVariable Long id) {
        boolean deleted = providerService.deleteProvider(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{providerId}/products")
    public ResponseEntity<List<ProviderProductResponse>> getProductsByProvider(
            @PathVariable Long providerId) {
        return ResponseEntity.ok(providerService.getProductsByProviderId(providerId));
    }

    @PostMapping("/{providerId}/products")
    public ResponseEntity<ProviderProductResponse> createProduct(
            @PathVariable Long providerId,
            @Valid @RequestBody ProviderProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerService.createProduct(providerId, request));
    }

    @DeleteMapping("/{providerId}/products/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long providerId,
            @PathVariable Long productId) {
        boolean deleted = providerService.deleteProduct(providerId, productId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
