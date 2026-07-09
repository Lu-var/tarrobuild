package cl.tarrobuild.hardwareadvisor.service;

import cl.tarrobuild.hardwareadvisor.client.BuildFeignClient;
import cl.tarrobuild.hardwareadvisor.client.CompatibilityFeignClient;
import cl.tarrobuild.hardwareadvisor.client.NotificationFeignClient;
import cl.tarrobuild.hardwareadvisor.client.ProductFeignClient;
import cl.tarrobuild.hardwareadvisor.dto.*;
import cl.tarrobuild.hardwareadvisor.model.Recommendation;
import cl.tarrobuild.hardwareadvisor.repository.RecommendationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HardwareAdvisorServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private BuildFeignClient buildFeignClient;

    @Mock
    private ProductFeignClient productFeignClient;

    @Mock
    private CompatibilityFeignClient compatibilityFeignClient;

    @Mock
    private NotificationFeignClient notificationFeignClient;

    @InjectMocks
    private HardwareAdvisorService service;

    @Captor
    private ArgumentCaptor<NotificationClientRequest> notificationCaptor;

    private static final Long BUILD_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final String BUILD_NAME = "Gaming PC Master Race";

    private BuildClientResponse build;
    private ProductClientResponse currentGpu;
    private ProductClientResponse currentCpu;
    private ProductClientResponse upgradeGpu;
    private ProductClientResponse upgradeGpuFlagship;
    private ProductClientResponse upgradeCpu;
    private ProductClientResponse inactiveProduct;
    private ProductClientResponse cheaperProduct;

    @BeforeEach
    void setUp() {
        BuildItemClientResponse itemGpu = new BuildItemClientResponse(10L, BUILD_ID, 100L, 1);
        BuildItemClientResponse itemCpu = new BuildItemClientResponse(11L, BUILD_ID, 101L, 1);

        build = new BuildClientResponse(
                BUILD_ID, USER_ID, BUILD_NAME, "COMPLETED",
                LocalDateTime.now(),
                List.of(itemGpu, itemCpu)
        );

        currentGpu = new ProductClientResponse(100L, "RTX 3060", "Mid-range GPU", 300, 1L,
                "NVIDIA", "RTX 3060", true, List.of());
        currentCpu = new ProductClientResponse(101L, "i5-12400", "Mid-range CPU", 200, 2L,
                "Intel", "i5-12400", true, List.of());

        upgradeGpu = new ProductClientResponse(102L, "RTX 4070", "High-end GPU", 500, 1L,
                "NVIDIA", "RTX 4070", true, List.of());
        upgradeGpuFlagship = new ProductClientResponse(103L, "RTX 4090", "Flagship GPU", 700, 1L,
                "NVIDIA", "RTX 4090", true, List.of());
        upgradeCpu = new ProductClientResponse(104L, "i7-13700", "High-end CPU", 350, 2L,
                "Intel", "i7-13700", true, List.of());
        inactiveProduct = new ProductClientResponse(105L, "GTX 1060", "Old GPU", 100, 1L,
                "NVIDIA", "GTX 1060", false, List.of());
        cheaperProduct = new ProductClientResponse(106L, "RTX 3050", "Entry GPU", 200, 1L,
                "NVIDIA", "RTX 3050", true, List.of());
    }

    private Recommendation createRec(Long id, String rule, Long suggestedProductId, String reason) {
        Recommendation rec = new Recommendation();
        rec.setId(id);
        rec.setBuildId(HardwareAdvisorServiceTest.BUILD_ID);
        rec.setRuleApplied(rule);
        rec.setSuggestedProductId(suggestedProductId);
        rec.setReason(reason);
        rec.setCreatedAt(LocalDateTime.now());
        return rec;
    }

    @Test
    @DisplayName("generate() — build con items, compatibilidad ok, upgrades encontrados, notificación enviada")
    void generate_withBuildAndUpgrades_shouldReturnRecommendationsAndNotify() {
        when(buildFeignClient.getBuildById(BUILD_ID)).thenReturn(build);
        when(compatibilityFeignClient.checkCompatibility(any()))
                .thenReturn(new CompatibilityCheckResponse(1L, BUILD_ID, true, "All compatible", LocalDateTime.now()));
        when(productFeignClient.getProductById(100L)).thenReturn(currentGpu);
        when(productFeignClient.getProductById(101L)).thenReturn(currentCpu);
        when(productFeignClient.getProductsByCategory(1L))
                .thenReturn(List.of(currentGpu, upgradeGpu, upgradeGpuFlagship, inactiveProduct, cheaperProduct));
        when(productFeignClient.getProductsByCategory(2L))
                .thenReturn(List.of(currentCpu, upgradeCpu));

        List<Recommendation> savedList = List.of(
                createRec(1L, "UPGRADE", 102L,
                        "Consider upgrading RTX 3060 (NVIDIA RTX 3060) to RTX 4070 (NVIDIA RTX 4070) for +$200"),
                createRec(2L, "UPGRADE", 103L,
                        "Consider upgrading RTX 3060 (NVIDIA RTX 3060) to RTX 4090 (NVIDIA RTX 4090) for +$400"),
                createRec(3L, "UPGRADE", 104L,
                        "Consider upgrading i5-12400 (Intel i5-12400) to i7-13700 (Intel i7-13700) for +$150")
        );
        when(recommendationRepository.saveAll(any())).thenReturn(savedList);

        List<RecommendationResponse> result = service.generate(new GenerateRecommendationsRequest(BUILD_ID));

        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(r -> "UPGRADE".equals(r.ruleApplied())));

        verify(notificationFeignClient).sendNotification(notificationCaptor.capture());
        NotificationClientRequest sent = notificationCaptor.getValue();
        assertEquals(USER_ID, sent.userId());
        assertEquals("RECOMMENDATION", sent.type());
        assertEquals("INFO", sent.status());
        assertTrue(sent.content().contains("3"));
        assertTrue(sent.content().contains(BUILD_NAME));
    }

    @Test
    @DisplayName("generate() — build no encontrado retorna lista vacía")
    void generate_buildNotFound_shouldReturnEmptyList() {
        when(buildFeignClient.getBuildById(BUILD_ID)).thenReturn(null);

        List<RecommendationResponse> result = service.generate(new GenerateRecommendationsRequest(BUILD_ID));

        assertTrue(result.isEmpty());
        verifyNoInteractions(compatibilityFeignClient, productFeignClient,
                recommendationRepository, notificationFeignClient);
    }

    @Test
    @DisplayName("generate() — build sin items retorna lista vacía")
    void generate_buildWithNoItems_shouldReturnEmptyList() {
        BuildClientResponse emptyBuild = new BuildClientResponse(
                BUILD_ID, USER_ID, BUILD_NAME, "COMPLETED",
                LocalDateTime.now(), List.of()
        );
        when(buildFeignClient.getBuildById(BUILD_ID)).thenReturn(emptyBuild);

        List<RecommendationResponse> result = service.generate(new GenerateRecommendationsRequest(BUILD_ID));

        assertTrue(result.isEmpty());
        verifyNoInteractions(compatibilityFeignClient, productFeignClient);
        verify(recommendationRepository, never()).saveAll(any());
        verify(notificationFeignClient, never()).sendNotification(any());
    }

    @Test
    @DisplayName("generate() — fallo de compatibilidad crea recomendación INCOMPATIBILITY")
    void generate_compatFailure_shouldCreateIncompatibilityRecommendation() {
        when(buildFeignClient.getBuildById(BUILD_ID)).thenReturn(build);
        when(compatibilityFeignClient.checkCompatibility(any()))
                .thenReturn(new CompatibilityCheckResponse(1L, BUILD_ID, false,
                        "Motherboard incompatible with CPU", LocalDateTime.now()));
        when(productFeignClient.getProductById(100L)).thenReturn(currentGpu);
        when(productFeignClient.getProductById(101L)).thenReturn(currentCpu);
        when(productFeignClient.getProductsByCategory(1L)).thenReturn(List.of(currentGpu));
        when(productFeignClient.getProductsByCategory(2L)).thenReturn(List.of(currentCpu));

        Recommendation compatRec = createRec(1L, "INCOMPATIBILITY", null,
                "Build has compatibility issues: Motherboard incompatible with CPU");
        when(recommendationRepository.saveAll(any())).thenReturn(List.of(compatRec));

        List<RecommendationResponse> result = service.generate(new GenerateRecommendationsRequest(BUILD_ID));

        assertEquals(1, result.size());
        assertEquals("INCOMPATIBILITY", result.getFirst().ruleApplied());
        assertNull(result.getFirst().suggestedProductId());
        assertEquals("Build has compatibility issues: Motherboard incompatible with CPU",
                result.getFirst().reason());

        verify(notificationFeignClient).sendNotification(any());
    }

    @Test
    @DisplayName("generate() — sin upgrades disponibles para productos actuales")
    void generate_noUpgradesAvailable_shouldReturnEmptyList() {
        when(buildFeignClient.getBuildById(BUILD_ID)).thenReturn(build);
        when(compatibilityFeignClient.checkCompatibility(any()))
                .thenReturn(new CompatibilityCheckResponse(1L, BUILD_ID, true, "All compatible", LocalDateTime.now()));
        when(productFeignClient.getProductById(100L)).thenReturn(currentGpu);
        when(productFeignClient.getProductById(101L)).thenReturn(currentCpu);
        when(productFeignClient.getProductsByCategory(1L))
                .thenReturn(List.of(currentGpu, cheaperProduct, inactiveProduct));
        when(productFeignClient.getProductsByCategory(2L))
                .thenReturn(List.of(currentCpu));

        when(recommendationRepository.saveAll(any())).thenReturn(List.of());

        List<RecommendationResponse> result = service.generate(new GenerateRecommendationsRequest(BUILD_ID));

        assertTrue(result.isEmpty());
        verify(notificationFeignClient, never()).sendNotification(any());
    }

    @Test
    @DisplayName("getRecommendationsByBuildId — retorna lista de recomendaciones")
    void getRecommendationsByBuildId_shouldReturnList() {
        List<Recommendation> recs = List.of(
                createRec(1L, "UPGRADE", 102L, "Upgrade GPU"),
                createRec(2L, "INCOMPATIBILITY", null, "Incompatible parts")
        );
        when(recommendationRepository.findByBuildId(BUILD_ID)).thenReturn(recs);

        List<RecommendationResponse> result = service.getRecommendationsByBuildId(BUILD_ID);

        assertEquals(2, result.size());
        assertEquals(1L, result.getFirst().id());
        assertEquals("UPGRADE", result.getFirst().ruleApplied());
        assertEquals(2L, result.get(1).id());
        assertEquals("INCOMPATIBILITY", result.get(1).ruleApplied());
    }

    @Test
    @DisplayName("getRecommendationById — recomendación encontrada retorna RecommendationResponse")
    void getRecommendationById_found_shouldReturnRecommendation() {
        Recommendation rec = createRec(1L, "UPGRADE", 102L, "Upgrade GPU");
        when(recommendationRepository.findById(1L)).thenReturn(Optional.of(rec));

        RecommendationResponse result = service.getRecommendationById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("UPGRADE", result.ruleApplied());
        assertEquals(102L, result.suggestedProductId());
    }

    @Test
    @DisplayName("getRecommendationById — recomendación no encontrada lanza EntityNotFoundException")
    void getRecommendationById_notFound_shouldThrowEntityNotFoundException() {
        when(recommendationRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> service.getRecommendationById(99L));
        assertTrue(ex.getMessage().contains("99"));
    }
}
