package cl.tarrobuild.provider.controller;

import cl.tarrobuild.provider.dto.ProviderProductRequest;
import cl.tarrobuild.provider.dto.ProviderProductResponse;
import cl.tarrobuild.provider.dto.ProviderRequest;
import cl.tarrobuild.provider.dto.ProviderResponse;
import cl.tarrobuild.provider.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
@Tag(name = "Providers", description = "Gestión de proveedores y referencias externas")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo proveedor", description = "Registra un nuevo proveedor en el sistema")
    public ResponseEntity<ProviderResponse> createProvider(
            @Valid @RequestBody ProviderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerService.createProvider(request));
    }

    @GetMapping
    @Operation(summary = "Obtener todos los proveedores", description = "Retorna una lista completa de todos los proveedores registrados")
    public ResponseEntity<List<ProviderResponse>> getAllProviders() {
        return ResponseEntity.ok(providerService.getAllProviders());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener proveedor por ID", description = "Retorna los datos de un proveedor según su identificador único")
    public ResponseEntity<ProviderResponse> getProviderById(@PathVariable Long id) {
        return ResponseEntity.ok(providerService.getProviderById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar proveedor", description = "Reemplaza completamente los datos de un proveedor existente")
    public ResponseEntity<ProviderResponse> updateProvider(
            @PathVariable Long id,
            @Valid @RequestBody ProviderRequest request) {
        return ResponseEntity.ok(providerService.updateProvider(id, request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Actualizar parcialmente proveedor", description = "Actualiza campos específicos de un proveedor sin reemplazar todo el recurso")
    public ResponseEntity<ProviderResponse> patchProvider(
            @PathVariable Long id,
            @RequestBody ProviderRequest request) {
        return ResponseEntity.ok(providerService.patchProvider(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar proveedor", description = "Elimina un proveedor del sistema por su ID")
    public ResponseEntity<Void> deleteProvider(@PathVariable Long id) {
        boolean deleted = providerService.deleteProvider(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{providerId}/products")
    @Operation(summary = "Obtener productos por proveedor", description = "Retorna la lista de productos asociados a un proveedor específico")
    public ResponseEntity<List<ProviderProductResponse>> getProductsByProvider(
            @PathVariable Long providerId) {
        return ResponseEntity.ok(providerService.getProductsByProviderId(providerId));
    }

    @PostMapping("/{providerId}/products")
    @Operation(summary = "Crear producto para proveedor", description = "Registra un nuevo producto asociado a un proveedor existente")
    public ResponseEntity<ProviderProductResponse> createProduct(
            @PathVariable Long providerId,
            @Valid @RequestBody ProviderProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerService.createProduct(providerId, request));
    }

    @DeleteMapping("/{providerId}/products/{productId}")
    @Operation(summary = "Eliminar producto de proveedor", description = "Elimina un producto asociado a un proveedor específico")
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