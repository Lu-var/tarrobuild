package cl.tarrobuild.estimate.service;

import cl.tarrobuild.estimate.client.BuildRestClient;
import cl.tarrobuild.estimate.client.NotificationRestClient;
import cl.tarrobuild.estimate.client.ProductRestClient;
import cl.tarrobuild.estimate.dto.BuildClientResponse;
import cl.tarrobuild.estimate.dto.BuildItemClientResponse;
import cl.tarrobuild.estimate.dto.EstimateRequest;
import cl.tarrobuild.estimate.dto.EstimateResponse;
import cl.tarrobuild.estimate.dto.NotificationClientRequest;
import cl.tarrobuild.estimate.dto.ProductClientResponse;
import cl.tarrobuild.estimate.model.Estimate;
import cl.tarrobuild.estimate.repository.EstimateRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstimateService")
class EstimateServiceTest {

    @Mock
    private EstimateRepository estimateRepository;

    @Mock
    private BuildRestClient buildRestClient;

    @Mock
    private ProductRestClient productRestClient;

    @Mock
    private NotificationRestClient notificationRestClient;

    @InjectMocks
    private EstimateService estimateService;

    private EstimateRequest request;
    private BuildClientResponse buildWithItems;
    private ProductClientResponse product1;
    private ProductClientResponse product2;
    private Estimate savedEstimate;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2026, 7, 8, 12, 0, 0);

        BuildItemClientResponse item1 = new BuildItemClientResponse(1L, 10L, 2);
        BuildItemClientResponse item2 = new BuildItemClientResponse(2L, 20L, 3);

        buildWithItems = new BuildClientResponse(
                100L, 42L, "Mi Build", "READY", List.of(item1, item2));

        request = new EstimateRequest(100L, "USD");

        product1 = new ProductClientResponse(10L, "Producto A", 500);
        product2 = new ProductClientResponse(20L, "Producto B", 300);

        Estimate estimate = new Estimate();
        estimate.setBuildId(100L);
        estimate.setTotalCost(1900);
        estimate.setCurrency("USD");

        savedEstimate = new Estimate();
        savedEstimate.setId(1L);
        savedEstimate.setBuildId(100L);
        savedEstimate.setTotalCost(1900);
        savedEstimate.setCurrency("USD");
        savedEstimate.setCreatedAt(now);
    }

    @Test
    @DisplayName("calculate: calcula costo total, guarda y retorna respuesta")
    void calculate_happyPath_returnsEstimateResponse() {
        when(buildRestClient.getBuildById(100L)).thenReturn(buildWithItems);
        when(productRestClient.getProductById(10L)).thenReturn(product1);
        when(productRestClient.getProductById(20L)).thenReturn(product2);
        when(estimateRepository.save(any(Estimate.class))).thenReturn(savedEstimate);

        EstimateResponse response = estimateService.calculate(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.buildId()).isEqualTo(100L);
        assertThat(response.totalCost()).isEqualTo(1900);
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.createdAt()).isEqualTo(now);

        ArgumentCaptor<Estimate> estimateCaptor = ArgumentCaptor.forClass(Estimate.class);
        verify(estimateRepository).save(estimateCaptor.capture());
        Estimate captured = estimateCaptor.getValue();
        assertThat(captured.getBuildId()).isEqualTo(100L);
        assertThat(captured.getTotalCost()).isEqualTo(1900);
        assertThat(captured.getCurrency()).isEqualTo("USD");

        ArgumentCaptor<NotificationClientRequest> notificationCaptor =
                ArgumentCaptor.forClass(NotificationClientRequest.class);
        verify(notificationRestClient).sendNotification(notificationCaptor.capture());
        NotificationClientRequest notification = notificationCaptor.getValue();
        assertThat(notification.userId()).isEqualTo(42L);
        assertThat(notification.type()).isEqualTo("ESTIMATE");
        assertThat(notification.content()).contains("Mi Build", "1900");
        assertThat(notification.status()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("calculate: asigna USD por defecto cuando currency es null")
    void calculate_nullCurrency_defaultsToUsd() {
        EstimateRequest requestWithoutCurrency = new EstimateRequest(100L, null);

        when(buildRestClient.getBuildById(100L)).thenReturn(buildWithItems);
        when(productRestClient.getProductById(10L)).thenReturn(product1);
        when(productRestClient.getProductById(20L)).thenReturn(product2);

        Estimate savedWithDefaults = new Estimate();
        savedWithDefaults.setId(2L);
        savedWithDefaults.setBuildId(100L);
        savedWithDefaults.setTotalCost(1900);
        savedWithDefaults.setCurrency("USD");
        savedWithDefaults.setCreatedAt(now);
        when(estimateRepository.save(any(Estimate.class))).thenReturn(savedWithDefaults);

        EstimateResponse response =
                estimateService.calculate(requestWithoutCurrency);

        assertThat(response.currency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("calculate: lanza EntityNotFoundException cuando build no existe")
    void calculate_buildNotFound_throwsEntityNotFoundException() {
        when(buildRestClient.getBuildById(100L))
                .thenThrow(new EntityNotFoundException("Build with ID 100 not found"));

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> estimateService.calculate(request));

        assertThat(ex.getMessage()).contains("100");
        verify(estimateRepository, never()).save(any());
        verify(notificationRestClient, never()).sendNotification(any());
    }

    @Test
    @DisplayName("calculate: lanza excepción cuando producto no existe")
    void calculate_productNotFound_throwsException() {
        when(buildRestClient.getBuildById(100L)).thenReturn(buildWithItems);
        when(productRestClient.getProductById(10L))
                .thenThrow(new EntityNotFoundException("Product with ID 10 not found"));

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> estimateService.calculate(request));

        assertThat(ex.getMessage()).contains("10");
        verify(productRestClient, never()).getProductById(20L);
        verify(estimateRepository, never()).save(any());
        verify(notificationRestClient, never()).sendNotification(any());
    }

    @Test
    @DisplayName("calculate: build sin items retorna costo 0")
    void calculate_buildWithNoItems_returnsZeroCost() {
        BuildClientResponse emptyBuild = new BuildClientResponse(
                100L, 42L, "Vacio", "READY", List.of());

        when(buildRestClient.getBuildById(100L)).thenReturn(emptyBuild);

        Estimate zeroEstimate = new Estimate();
        zeroEstimate.setId(3L);
        zeroEstimate.setBuildId(100L);
        zeroEstimate.setTotalCost(0);
        zeroEstimate.setCurrency("USD");
        zeroEstimate.setCreatedAt(now);
        when(estimateRepository.save(any(Estimate.class))).thenReturn(zeroEstimate);

        EstimateResponse response = estimateService.calculate(request);

        assertThat(response.totalCost()).isZero();
        verify(productRestClient, never()).getProductById(any());
    }

    @Test
    @DisplayName("calculate: fallo en notificación no interrumpe el flujo")
    void calculate_notificationFails_doesNotThrow() {
        when(buildRestClient.getBuildById(100L)).thenReturn(buildWithItems);
        when(productRestClient.getProductById(10L)).thenReturn(product1);
        when(productRestClient.getProductById(20L)).thenReturn(product2);
        when(estimateRepository.save(any(Estimate.class))).thenReturn(savedEstimate);
        doThrow(new RuntimeException("Notification service down"))
                .when(notificationRestClient).sendNotification(any());

        EstimateResponse response = estimateService.calculate(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getLatestEstimateByBuildId: retorna el más reciente cuando existe")
    void getLatestEstimateByBuildId_found_returnsEstimateResponse() {
        when(estimateRepository.findTopByBuildIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(savedEstimate));

        EstimateResponse response =
                estimateService.getLatestEstimateByBuildId(100L);

        assertThat(response).isNotNull();
        assertThat(response.buildId()).isEqualTo(100L);
        assertThat(response.totalCost()).isEqualTo(1900);
    }

    @Test
    @DisplayName("getLatestEstimateByBuildId: lanza EntityNotFoundException cuando no existe")
    void getLatestEstimateByBuildId_notFound_throwsEntityNotFoundException() {
        when(estimateRepository.findTopByBuildIdOrderByCreatedAtDesc(99L))
                .thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> estimateService.getLatestEstimateByBuildId(99L));

        assertThat(ex.getMessage()).contains("99");
    }

    @Test
    @DisplayName("getEstimateById: retorna estimate cuando existe")
    void getEstimateById_found_returnsEstimateResponse() {
        when(estimateRepository.findById(1L))
                .thenReturn(Optional.of(savedEstimate));

        EstimateResponse response = estimateService.getEstimateById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.buildId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getEstimateById: lanza EntityNotFoundException cuando no existe")
    void getEstimateById_notFound_throwsEntityNotFoundException() {
        when(estimateRepository.findById(999L))
                .thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> estimateService.getEstimateById(999L));

        assertThat(ex.getMessage()).contains("999");
    }

    @Test
    @DisplayName("getEstimatesByBuildId: retorna lista vacía cuando no hay estimates")
    void getEstimatesByBuildId_emptyList_returnsEmptyList() {
        when(estimateRepository.findByBuildId(100L))
                .thenReturn(List.of());

        List<EstimateResponse> responses =
                estimateService.getEstimatesByBuildId(100L);

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("getEstimatesByBuildId: retorna lista de estimates")
    void getEstimatesByBuildId_returnsList() {
        Estimate estimate2 = new Estimate();
        estimate2.setId(2L);
        estimate2.setBuildId(100L);
        estimate2.setTotalCost(2500);
        estimate2.setCurrency("EUR");
        estimate2.setCreatedAt(now.plusHours(1));

        when(estimateRepository.findByBuildId(100L))
                .thenReturn(List.of(savedEstimate, estimate2));

        List<EstimateResponse> responses =
                estimateService.getEstimatesByBuildId(100L);

        assertThat(responses).hasSize(2);
        assertThat(responses.getFirst().id()).isEqualTo(1L);
        assertThat(responses.getFirst().totalCost()).isEqualTo(1900);
        assertThat(responses.getFirst().currency()).isEqualTo("USD");
        assertThat(responses.get(1).id()).isEqualTo(2L);
        assertThat(responses.get(1).totalCost()).isEqualTo(2500);
        assertThat(responses.get(1).currency()).isEqualTo("EUR");
    }
}
