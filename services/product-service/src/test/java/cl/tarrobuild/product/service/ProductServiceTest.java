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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAttributeRepository productAttributeRepository;

    @Mock
    private CategoryValidationService categoryValidationService;

    @InjectMocks
    private ProductService productService;

    private ProductRequest productRequest;
    private Product savedProduct;
    private ProductAttributeRequest attrRequest;
    private ProductAttribute savedAttribute;

    @BeforeEach
    void setUp() {
        productRequest = new ProductRequest(
                "Procesador AMD Ryzen 7",
                "Procesador de 8 núcleos y 16 hilos",
                350000,
                1L,
                "AMD",
                "Ryzen 7 5700X",
                true
        );

        savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName(productRequest.name());
        savedProduct.setDescription(productRequest.description());
        savedProduct.setMsrp(productRequest.msrp());
        savedProduct.setCategoryId(productRequest.categoryId());
        savedProduct.setBrand(productRequest.brand());
        savedProduct.setModel(productRequest.model());
        savedProduct.setIsActive(productRequest.isActive());

        attrRequest = new ProductAttributeRequest("Socket", "AM4");

        savedAttribute = new ProductAttribute();
        savedAttribute.setId(10L);
        savedAttribute.setAttributeName("Socket");
        savedAttribute.setAttributeValue("AM4");
        savedAttribute.setProduct(savedProduct);
    }

    // ─────────────────────────────────────────────────────────────────
    // CREAR PRODUCTO
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debería crear un producto y devolver la respuesta con ID asignado")
    void createProduct_ShouldCreateAndReturnProduct() {
        doNothing().when(categoryValidationService).validateCategoryExists(anyLong());
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(productRequest);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Procesador AMD Ryzen 7", response.name());
        assertEquals(350000, response.msrp());
        assertEquals("AMD", response.brand());
        assertTrue(response.isActive());
        verify(categoryValidationService).validateCategoryExists(1L);
        verify(productRepository).save(any(Product.class));
    }

    // ─────────────────────────────────────────────────────────────────
    // OBTENER PRODUCTO POR ID
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debería devolver un producto existente por su ID")
    void getProductById_ShouldReturnProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(savedProduct));

        ProductResponse response = productService.getProductById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Procesador AMD Ryzen 7", response.name());
    }

    @Test
    @DisplayName("Debería lanzar EntityNotFoundException cuando el producto no existe")
    void getProductById_ShouldThrowWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> productService.getProductById(99L));
    }

    // ─────────────────────────────────────────────────────────────────
    // LISTAR TODOS LOS PRODUCTOS ACTIVOS
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debería devolver solo los productos activos")
    void getAllProducts_ShouldReturnOnlyActive() {
        when(productRepository.findByIsActiveTrue()).thenReturn(List.of(savedProduct));

        List<ProductResponse> responses = productService.getAllProducts();

        assertEquals(1, responses.size());
        assertEquals("Procesador AMD Ryzen 7", responses.getFirst().name());
    }

    // ─────────────────────────────────────────────────────────────────
    // ACTUALIZAR PRODUCTO
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debería actualizar un producto existente")
    void updateProduct_ShouldUpdateAndReturn() {
        ProductRequest updateRequest = new ProductRequest(
                "Procesador AMD Ryzen 9",
                "Procesador de 12 núcleos y 24 hilos",
                500000,
                1L,
                "AMD",
                "Ryzen 9 5900X",
                true
        );

        doNothing().when(categoryValidationService).validateCategoryExists(anyLong());
        when(productRepository.findById(1L)).thenReturn(Optional.of(savedProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.updateProduct(1L, updateRequest);

        assertNotNull(response);
        assertEquals("Procesador AMD Ryzen 9", response.name());
        assertEquals(500000, response.msrp());
        assertEquals("Ryzen 9 5900X", response.model());
        verify(categoryValidationService).validateCategoryExists(1L);
    }

    @Test
    @DisplayName("Debería lanzar EntityNotFoundException al actualizar un producto inexistente")
    void updateProduct_ShouldThrowWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> productService.updateProduct(99L, productRequest));
    }

    // ─────────────────────────────────────────────────────────────────
    // ELIMINAR PRODUCTO
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debería eliminar un producto y retornar true")
    void deleteProduct_ShouldDeleteAndReturnTrue() {
        when(productRepository.existsById(1L)).thenReturn(true);

        boolean result = productService.deleteProduct(1L);

        assertTrue(result);
        verify(productRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Debería retornar false al intentar eliminar un producto inexistente")
    void deleteProduct_ShouldReturnFalseWhenNotFound() {
        when(productRepository.existsById(99L)).thenReturn(false);

        boolean result = productService.deleteProduct(99L);

        assertFalse(result);
        verify(productRepository, never()).deleteById(anyLong());
    }

    // ─────────────────────────────────────────────────────────────────
    // ACTIVAR / DESACTIVAR PRODUCTO
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debería activar un producto desactivado")
    void activateProduct_ShouldActivate() {
        savedProduct.setIsActive(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(savedProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.activateProduct(1L);

        assertTrue(savedProduct.getIsActive());
        verify(productRepository).save(savedProduct);
    }

    @Test
    @DisplayName("Debería desactivar un producto activo")
    void deactivateProduct_ShouldDeactivate() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(savedProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.deactivateProduct(1L);

        assertFalse(savedProduct.getIsActive());
        verify(productRepository).save(savedProduct);
    }

    // ─────────────────────────────────────────────────────────────────
    // FILTROS
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debería filtrar productos por categoría")
    void getProductsByCategory_ShouldReturnProducts() {
        when(productRepository.findByCategoryId(1L)).thenReturn(List.of(savedProduct));

        List<ProductResponse> responses = productService.getProductsByCategory(1L);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.getFirst().categoryId());
    }

    @Test
    @DisplayName("Debería filtrar productos por marca (case-insensitive)")
    void getProductsByBrand_ShouldReturnProducts() {
        when(productRepository.findByBrandIgnoreCase("amd")).thenReturn(List.of(savedProduct));

        List<ProductResponse> responses = productService.getProductsByBrand("amd");

        assertEquals(1, responses.size());
        assertEquals("AMD", responses.getFirst().brand());
    }

    @Test
    @DisplayName("Debería filtrar productos por rango de precio MSRP")
    void getProductsByMsrpRange_ShouldReturnProducts() {
        when(productRepository.findByMsrpBetween(200000, 400000)).thenReturn(List.of(savedProduct));

        List<ProductResponse> responses = productService.getProductsByMsrpRange(200000, 400000);

        assertEquals(1, responses.size());
        assertEquals(350000, responses.getFirst().msrp());
    }

    // ─────────────────────────────────────────────────────────────────
    // ATRIBUTOS — OBTENER
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debería obtener los atributos de un producto existente")
    void getProductAttributes_ShouldReturnAttributes() {
        savedProduct.getAttributes().add(savedAttribute);

        when(productRepository.existsById(1L)).thenReturn(true);
        when(productAttributeRepository.findByProductId(1L)).thenReturn(List.of(savedAttribute));

        List<ProductAttributeResponse> responses = productService.getProductAttributes(1L);

        assertEquals(1, responses.size());
        assertEquals("Socket", responses.getFirst().attributeName());
        assertEquals("AM4", responses.getFirst().attributeValue());
        assertEquals(1L, responses.getFirst().productId());
    }

    @Test
    @DisplayName("Debería lanzar EntityNotFoundException al obtener atributos de un producto inexistente")
    void getProductAttributes_ShouldThrowWhenProductNotFound() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class,
                () -> productService.getProductAttributes(99L));
    }

    // ─────────────────────────────────────────────────────────────────
    // ATRIBUTOS — AGREGAR
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debería agregar un atributo a un producto existente")
    void addProductAttribute_ShouldAddAndReturn() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(savedProduct));
        when(productAttributeRepository.existsByAttributeNameAndProductId("Socket", 1L)).thenReturn(false);
        when(productAttributeRepository.save(any(ProductAttribute.class))).thenReturn(savedAttribute);

        ProductAttributeResponse response = productService.addProductAttribute(1L, attrRequest);

        assertNotNull(response);
        assertEquals("Socket", response.attributeName());
        assertEquals("AM4", response.attributeValue());
        assertEquals(1L, response.productId());
    }

    @Test
    @DisplayName("Debería lanzar EntityExistsException al agregar un atributo duplicado")
    void addProductAttribute_ShouldThrowWhenDuplicate() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(savedProduct));
        when(productAttributeRepository.existsByAttributeNameAndProductId("Socket", 1L)).thenReturn(true);

        assertThrows(EntityExistsException.class,
                () -> productService.addProductAttribute(1L, attrRequest));

        verify(productAttributeRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────
    // ATRIBUTOS — ACTUALIZAR
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debería actualizar un atributo existente de un producto")
    void updateProductAttribute_ShouldUpdateAndReturn() {
        ProductAttributeRequest updateRequest = new ProductAttributeRequest("Socket", "LGA1700");

        when(productAttributeRepository.findByIdAndProductId(10L, 1L))
                .thenReturn(Optional.of(savedAttribute));
        when(productAttributeRepository.save(any(ProductAttribute.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProductAttributeResponse response = productService.updateProductAttribute(1L, 10L, updateRequest);

        assertEquals("Socket", response.attributeName());
        assertEquals("LGA1700", response.attributeValue());
    }

    // ─────────────────────────────────────────────────────────────────
    // ATRIBUTOS — ELIMINAR
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debería eliminar un atributo y retornar true")
    void deleteProductAttribute_ShouldDeleteAndReturnTrue() {
        when(productAttributeRepository.existsById(10L)).thenReturn(true);
        when(productAttributeRepository.findByIdAndProductId(10L, 1L))
                .thenReturn(Optional.of(savedAttribute));

        boolean result = productService.deleteProductAttribute(1L, 10L);

        assertTrue(result);
        verify(productAttributeRepository).deleteById(10L);
    }

    @Test
    @DisplayName("Debería retornar false al eliminar un atributo que no pertenece al producto")
    void deleteProductAttribute_ShouldReturnFalseWhenNotBelongsToProduct() {
        when(productAttributeRepository.existsById(10L)).thenReturn(true);
        when(productAttributeRepository.findByIdAndProductId(10L, 2L))
                .thenReturn(Optional.empty());

        boolean result = productService.deleteProductAttribute(2L, 10L);

        assertFalse(result);
        verify(productAttributeRepository, never()).deleteById(anyLong());
    }
}
