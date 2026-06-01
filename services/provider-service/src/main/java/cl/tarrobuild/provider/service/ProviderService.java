package cl.tarrobuild.provider.service;

import cl.tarrobuild.provider.dto.ProviderProductRequest;
import cl.tarrobuild.provider.dto.ProviderProductResponse;
import cl.tarrobuild.provider.dto.ProviderRequest;
import cl.tarrobuild.provider.dto.ProviderResponse;
import cl.tarrobuild.provider.model.Provider;
import cl.tarrobuild.provider.model.ProviderProduct;
import cl.tarrobuild.provider.repository.ProviderProductRepository;
import cl.tarrobuild.provider.repository.ProviderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final ProviderProductRepository providerProductRepository;

    public ProviderService(ProviderRepository providerRepository,
                           ProviderProductRepository providerProductRepository) {
        this.providerRepository = providerRepository;
        this.providerProductRepository = providerProductRepository;
    }

    public ProviderResponse createProvider(ProviderRequest request) {
        log.info("Creating provider \"{}\"", request.name());

        Provider provider = new Provider();
        provider.setName(request.name());
        provider.setContact(request.contact());
        provider.setWebsite(request.website());
        provider.setIsActive(request.isActive() != null ? request.isActive() : true);

        Provider saved = providerRepository.save(provider);
        log.info("Provider created with id: {}", saved.getId());
        return toResponse(saved);
    }

    public List<ProviderResponse> getAllProviders() {
        log.info("Getting all providers");
        return providerRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProviderResponse getProviderById(Long id) {
        log.info("Getting provider by id: {}", id);
        return providerRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Provider with ID " + id + " not found"));
    }

    public ProviderResponse updateProvider(Long id, ProviderRequest request) {
        log.info("Updating provider id: {}", id);
        return providerRepository.findById(id)
                .map(provider -> {
                    provider.setName(request.name());
                    provider.setContact(request.contact());
                    provider.setWebsite(request.website());
                    if (request.isActive() != null) {
                        provider.setIsActive(request.isActive());
                    }
                    Provider saved = providerRepository.save(provider);
                    log.info("Provider with id: {} updated", id);
                    return toResponse(saved);
                })
                .orElseThrow(() -> new EntityNotFoundException(
                        "Provider with ID " + id + " not found"));
    }

    public boolean deleteProvider(Long id) {
        if (!providerRepository.existsById(id)) {
            log.info("Provider with id: {} not found for deletion", id);
            return false;
        }
        providerRepository.deleteById(id);
        log.info("Provider with id: {} deleted", id);
        return true;
    }

    public List<ProviderProductResponse> getProductsByProviderId(Long providerId) {
        log.info("Getting products for provider id: {}", providerId);
        providerRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Provider with ID " + providerId + " not found"));

        return providerProductRepository.findByProviderId(providerId).stream()
                .map(this::toProductResponse)
                .toList();
    }

    public ProviderProductResponse createProduct(Long providerId, ProviderProductRequest request) {
        log.info("Creating product reference for provider id: {}", providerId);
        providerRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Provider with ID " + providerId + " not found"));

        ProviderProduct product = new ProviderProduct();
        product.setProviderId(providerId);
        product.setProductId(request.productId());
        product.setExternalReference(request.externalReference());

        ProviderProduct saved = providerProductRepository.save(product);
        log.info("Product reference created with id: {} for provider id: {}", saved.getId(), providerId);
        return toProductResponse(saved);
    }

    public boolean deleteProduct(Long providerId, Long productId) {
        log.info("Deleting product reference id: {} for provider id: {}", productId, providerId);
        ProviderProduct product = providerProductRepository.findByIdAndProviderId(productId, providerId)
                .orElse(null);
        if (product == null) {
            log.info("Product reference with id: {} not found for provider id: {}", productId, providerId);
            return false;
        }
        providerProductRepository.delete(product);
        log.info("Product reference with id: {} deleted", productId);
        return true;
    }

    private ProviderResponse toResponse(Provider provider) {
        return new ProviderResponse(
                provider.getId(),
                provider.getName(),
                provider.getContact(),
                provider.getWebsite(),
                provider.getIsActive()
        );
    }

    private ProviderProductResponse toProductResponse(ProviderProduct product) {
        return new ProviderProductResponse(
                product.getId(),
                product.getProviderId(),
                product.getProductId(),
                product.getExternalReference()
        );
    }
}
