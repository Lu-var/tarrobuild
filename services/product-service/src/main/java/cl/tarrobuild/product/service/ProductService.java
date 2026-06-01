package cl.tarrobuild.product.service;

import cl.tarrobuild.product.dto.ProductAttributeRequest;
import cl.tarrobuild.product.dto.ProductAttributeResponse;
import cl.tarrobuild.product.dto.ProductRequest;
import cl.tarrobuild.product.dto.ProductResponse;
import cl.tarrobuild.product.model.Product;
import cl.tarrobuild.product.model.ProductAttribute;
import cl.tarrobuild.product.repository.ProductAttributeRepository;
import cl.tarrobuild.product.repository.ProductRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final CategoryValidationService categoryValidationService;

    public ProductService(ProductRepository productRepository,
                          ProductAttributeRepository productAttributeRepository,
                          CategoryValidationService categoryValidationService) {
        this.productRepository = productRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.categoryValidationService = categoryValidationService;
    }

    public List<ProductResponse> getAllProducts() {
        log.info("Getting all active products");
        return productRepository.findByIsActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        log.info("Getting products by category id: {}", categoryId);
        return productRepository.findByCategoryId(categoryId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProductResponse> getProductsByBrand(String brand) {
        log.info("Getting products by brand: {}", brand);
        return productRepository.findByBrandIgnoreCase(brand)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProductResponse> getProductsByMsrpRange(Integer minMsrp, Integer maxMsrp) {
        log.info("Getting products by msrp range: {} - {}", minMsrp, maxMsrp);
        return productRepository.findByMsrpBetween(minMsrp, maxMsrp)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse getProductById(Long id) {
        log.info("Getting product by id: {}", id);
        return productRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Product with ID " + id + " not found"));
    }

    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product \"{}\" categoryId: {}", request.name(), request.categoryId());

        categoryValidationService.validateCategoryExists(request.categoryId());

        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setMsrp(request.msrp());
        product.setCategoryId(request.categoryId());
        product.setBrand(request.brand());
        product.setModel(request.model());
        product.setIsActive(request.isActive() != null ? request.isActive() : true);

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product id: {}", id);

        categoryValidationService.validateCategoryExists(request.categoryId());

        return productRepository.findById(id)
                .map(product -> {
                    product.setName(request.name());
                    product.setDescription(request.description());
                    product.setMsrp(request.msrp());
                    product.setCategoryId(request.categoryId());
                    product.setBrand(request.brand());
                    product.setModel(request.model());
                    if (request.isActive() != null) {
                        product.setIsActive(request.isActive());
                    }

                    Product saved = productRepository.save(product);
                    return toResponse(saved);
                })
                .orElseThrow(() -> new EntityNotFoundException("Product with ID " + id + " not found"));
    }

    public boolean deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            log.info("Product with id: {} not found for deletion", id);
            return false;
        }
        productRepository.deleteById(id);
        log.info("Product with id: {} deleted successfully", id);
        return true;
    }

    public void activateProduct(Long id) {
        log.info("Activating product id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product with ID " + id + " not found"));

        product.setIsActive(true);

        productRepository.save(product);

        log.info("Product with id: {} activated", id);
    }

    public void deactivateProduct(Long id) {
        log.info("Deactivating product id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product with ID " + id + " not found"));

        product.setIsActive(false);

        productRepository.save(product);

        log.info("Product with id: {} deactivated", id);
    }

    // Product Attributes

    public List<ProductAttributeResponse> getProductAttributes(Long productId) {
        log.info("Getting attributes for product id: {}", productId);
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("Product with ID " + productId + " not found");
        }
        return productAttributeRepository.findByProductId(productId)
                .stream()
                .map(attr -> new ProductAttributeResponse(
                        attr.getId(),
                        attr.getAttributeName(),
                        attr.getAttributeValue(),
                        attr.getProduct().getId()
                ))
                .toList();
    }

    public ProductAttributeResponse addProductAttribute(Long productId, ProductAttributeRequest request) {
        log.info("Adding attribute to product id: {}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product with ID " + productId + " not found"));

        if (productAttributeRepository.existsByAttributeNameAndProductId(request.attributeName(), productId)) {
            throw new EntityExistsException("Attribute with name '" + request.attributeName() + "' already exists");
        }

        ProductAttribute attribute = new ProductAttribute();
        attribute.setAttributeName(request.attributeName());
        attribute.setAttributeValue(request.attributeValue());
        attribute.setProduct(product);

        ProductAttribute saved = productAttributeRepository.save(attribute);
        return new ProductAttributeResponse(
                saved.getId(),
                saved.getAttributeName(),
                saved.getAttributeValue(),
                saved.getProduct().getId()
        );
    }

    public ProductAttributeResponse updateProductAttribute(Long productId, Long attributeId, ProductAttributeRequest request) {
        log.info("Updating attribute id: {} for product id: {}", attributeId, productId);
        ProductAttribute attribute = productAttributeRepository.findByIdAndProductId(attributeId, productId)
                .orElseThrow(() -> new EntityNotFoundException("Attribute not found"));

        attribute.setAttributeName(request.attributeName());
        attribute.setAttributeValue(request.attributeValue());

        ProductAttribute saved = productAttributeRepository.save(attribute);
        return new ProductAttributeResponse(
                saved.getId(),
                saved.getAttributeName(),
                saved.getAttributeValue(),
                saved.getProduct().getId()
        );
    }

    public boolean deleteProductAttribute(Long productId, Long attributeId) {
        log.info("Deleting attribute id: {} for product id: {}", attributeId, productId);
        if (!productAttributeRepository.existsById(attributeId)) {
            return false;
        }
        if (!productAttributeRepository.findByIdAndProductId(attributeId, productId).isPresent()) {
            log.warn("Attribute {} does not belong to product {}", attributeId, productId);
            return false;
        }
        productAttributeRepository.deleteById(attributeId);
        return true;
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getMsrp(),
                product.getCategoryId(),
                product.getBrand(),
                product.getModel(),
                product.getIsActive(),
                product.getAttributes()
                        .stream()
                        .map(attr -> new ProductAttributeResponse(
                                attr.getId(),
                                attr.getAttributeName(),
                                attr.getAttributeValue(),
                                attr.getProduct().getId()
                        ))
                        .toList()
        );
    }
}