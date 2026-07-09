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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ProviderProductRepository providerProductRepository;

    @InjectMocks
    private ProviderService providerService;

    private Provider provider;
    private ProviderProduct providerProduct;
    private ProviderRequest createRequest;
    private ProviderRequest updateRequest;
    private ProviderRequest partialRequest;
    private ProviderProductRequest productRequest;

    @BeforeEach
    void setUp() {
        provider = new Provider();
        provider.setId(1L);
        provider.setName("Proveedor Test");
        provider.setContact("contacto@test.com");
        provider.setWebsite("https://test.com");
        provider.setIsActive(true);

        providerProduct = new ProviderProduct();
        providerProduct.setId(10L);
        providerProduct.setProvider(provider);
        providerProduct.setProductId(100L);
        providerProduct.setExternalReference("REF-001");

        createRequest = new ProviderRequest(
                "Proveedor Test",
                "contacto@test.com",
                "https://test.com",
                true
        );

        updateRequest = new ProviderRequest(
                "Proveedor Actualizado",
                "nuevo@test.com",
                "https://nuevo.com",
                false
        );

        partialRequest = new ProviderRequest(
                "Nombre Parcial",
                null,
                null,
                null
        );

        productRequest = new ProviderProductRequest(100L, "REF-001");
    }

    @Test
    @DisplayName("Crear proveedor exitosamente")
    void createProvider_success() {
        when(providerRepository.save(any(Provider.class))).thenReturn(provider);

        ProviderResponse response = providerService.createProvider(createRequest);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Proveedor Test", response.name());
        assertEquals("contacto@test.com", response.contact());
        assertEquals("https://test.com", response.website());
        assertTrue(response.isActive());

        verify(providerRepository).save(any(Provider.class));
    }

    @Test
    @DisplayName("Obtener todos los proveedores retorna lista")
    void getAllProviders_returnsList() {
        Provider provider2 = new Provider();
        provider2.setId(2L);
        provider2.setName("Proveedor Dos");
        provider2.setIsActive(true);

        when(providerRepository.findAll()).thenReturn(List.of(provider, provider2));

        List<ProviderResponse> responses = providerService.getAllProviders();

        assertEquals(2, responses.size());
        assertEquals(1L, responses.getFirst().id());
        assertEquals(2L, responses.get(1).id());

        verify(providerRepository).findAll();
    }

    @Test
    @DisplayName("Obtener proveedor por ID — encontrado")
    void getProviderById_found() {
        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));

        ProviderResponse response = providerService.getProviderById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Proveedor Test", response.name());

        verify(providerRepository).findById(1L);
    }

    @Test
    @DisplayName("Obtener proveedor por ID — no encontrado")
    void getProviderById_notFound_throwsException() {
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> providerService.getProviderById(99L));
        assertTrue(ex.getMessage().contains("99"));

        verify(providerRepository).findById(99L);
    }

    @Test
    @DisplayName("Actualizar proveedor — exitoso")
    void updateProvider_success() {
        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));

        Provider updatedProvider = new Provider();
        updatedProvider.setId(1L);
        updatedProvider.setName("Proveedor Actualizado");
        updatedProvider.setContact("nuevo@test.com");
        updatedProvider.setWebsite("https://nuevo.com");
        updatedProvider.setIsActive(false);

        when(providerRepository.save(any(Provider.class))).thenReturn(updatedProvider);

        ProviderResponse response = providerService.updateProvider(1L, updateRequest);

        assertNotNull(response);
        assertEquals("Proveedor Actualizado", response.name());
        assertEquals("nuevo@test.com", response.contact());
        assertEquals("https://nuevo.com", response.website());
        assertFalse(response.isActive());

        verify(providerRepository).findById(1L);
        verify(providerRepository).save(any(Provider.class));
    }

    @Test
    @DisplayName("Actualizar proveedor — no encontrado")
    void updateProvider_notFound_throwsException() {
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> providerService.updateProvider(99L, updateRequest));
        assertTrue(ex.getMessage().contains("99"));

        verify(providerRepository).findById(99L);
        verify(providerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar proveedor parcialmente — exitoso")
    void patchProvider_partialUpdate_success() {
        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));

        Provider patchedProvider = new Provider();
        patchedProvider.setId(1L);
        patchedProvider.setName("Nombre Parcial");
        patchedProvider.setContact("contacto@test.com");
        patchedProvider.setWebsite("https://test.com");
        patchedProvider.setIsActive(true);

        when(providerRepository.save(any(Provider.class))).thenReturn(patchedProvider);

        ProviderResponse response = providerService.patchProvider(1L, partialRequest);

        assertNotNull(response);
        assertEquals("Nombre Parcial", response.name());
        assertEquals("contacto@test.com", response.contact());
        assertEquals("https://test.com", response.website());
        assertTrue(response.isActive());

        verify(providerRepository).findById(1L);
        verify(providerRepository).save(any(Provider.class));
    }

    @Test
    @DisplayName("Actualizar proveedor parcialmente — no encontrado")
    void patchProvider_notFound_throwsException() {
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> providerService.patchProvider(99L, partialRequest));
        assertTrue(ex.getMessage().contains("99"));

        verify(providerRepository).findById(99L);
        verify(providerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Eliminar proveedor — exitoso retorna true")
    void deleteProvider_success_returnsTrue() {
        when(providerRepository.existsById(1L)).thenReturn(true);

        boolean result = providerService.deleteProvider(1L);

        assertTrue(result);
        verify(providerRepository).existsById(1L);
        verify(providerRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Eliminar proveedor — no encontrado retorna false")
    void deleteProvider_notFound_returnsFalse() {
        when(providerRepository.existsById(99L)).thenReturn(false);

        boolean result = providerService.deleteProvider(99L);

        assertFalse(result);
        verify(providerRepository).existsById(99L);
        verify(providerRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Obtener productos por ID de proveedor — proveedor no encontrado")
    void getProductsByProviderId_providerNotFound_throwsException() {
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> providerService.getProductsByProviderId(99L));
        assertTrue(ex.getMessage().contains("99"));

        verify(providerRepository).findById(99L);
        verify(providerProductRepository, never()).findByProvider_Id(any());
    }

    @Test
    @DisplayName("Obtener productos por ID de proveedor — exitoso")
    void getProductsByProviderId_success() {
        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(providerProductRepository.findByProvider_Id(1L)).thenReturn(List.of(providerProduct));

        List<ProviderProductResponse> responses = providerService.getProductsByProviderId(1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(10L, responses.getFirst().id());
        assertEquals(1L, responses.getFirst().providerId());
        assertEquals(100L, responses.getFirst().productId());
        assertEquals("REF-001", responses.getFirst().externalReference());

        verify(providerRepository).findById(1L);
        verify(providerProductRepository).findByProvider_Id(1L);
    }

    @Test
    @DisplayName("Crear producto — exitoso")
    void createProduct_success() {
        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));

        ProviderProduct savedProduct = new ProviderProduct();
        savedProduct.setId(10L);
        savedProduct.setProvider(provider);
        savedProduct.setProductId(100L);
        savedProduct.setExternalReference("REF-001");

        when(providerProductRepository.save(any(ProviderProduct.class))).thenReturn(savedProduct);

        ProviderProductResponse response = providerService.createProduct(1L, productRequest);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(1L, response.providerId());
        assertEquals(100L, response.productId());
        assertEquals("REF-001", response.externalReference());

        verify(providerRepository).findById(1L);
        verify(providerProductRepository).save(any(ProviderProduct.class));
    }

    @Test
    @DisplayName("Crear producto — proveedor no encontrado")
    void createProduct_providerNotFound_throwsException() {
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> providerService.createProduct(99L, productRequest));
        assertTrue(ex.getMessage().contains("99"));

        verify(providerRepository).findById(99L);
        verify(providerProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("Eliminar producto — exitoso retorna true")
    void deleteProduct_success_returnsTrue() {
        when(providerProductRepository.findByIdAndProvider_Id(10L, 1L))
                .thenReturn(Optional.of(providerProduct));

        boolean result = providerService.deleteProduct(1L, 10L);

        assertTrue(result);
        verify(providerProductRepository).findByIdAndProvider_Id(10L, 1L);
        verify(providerProductRepository).delete(providerProduct);
    }

    @Test
    @DisplayName("Eliminar producto — no encontrado retorna false")
    void deleteProduct_notFound_returnsFalse() {
        when(providerProductRepository.findByIdAndProvider_Id(99L, 1L))
                .thenReturn(Optional.empty());

        boolean result = providerService.deleteProduct(1L, 99L);

        assertFalse(result);
        verify(providerProductRepository).findByIdAndProvider_Id(99L, 1L);
        verify(providerProductRepository, never()).delete(any());
    }
}
