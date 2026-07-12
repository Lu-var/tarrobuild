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
import static org.mockito.ArgumentMatchers.anyLong;
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

    @BeforeEach
    void setUp() {
        lenient().when(productFeignClient.getProductsByCategory(anyLong())).thenReturn(List.of());

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
    @DisplayName("generate() — build con items, categorías vacías, recomienda componentes faltantes y notifica")
    void generate_withMissingCategories_shouldRecommendAndNotify() {
        when(buildFeignClient.getBuildById(BUILD_ID)).thenReturn(build);
        when(compatibilityFeignClient.checkCompatibility(any()))
                .thenReturn(new CompatibilityCheckResponse(1L, BUILD_ID, true, "All compatible", LocalDateTime.now()));
        when(productFeignClient.getProductById(100L)).thenReturn(currentGpu);
        when(productFeignClient.getProductById(101L)).thenReturn(currentCpu);

        ProductClientResponse ram = new ProductClientResponse(201L, "Corsair Vengeance", "32GB DDR5", 150, 3L, "Corsair", "Vengeance", true, List.of());
        lenient().when(productFeignClient.getProductsByCategory(3L)).thenReturn(List.of(ram));
        lenient().when(productFeignClient.getProductsByCategory(4L)).thenReturn(List.of(ram));
        lenient().when(productFeignClient.getProductsByCategory(5L)).thenReturn(List.of(ram));
        lenient().when(productFeignClient.getProductsByCategory(6L)).thenReturn(List.of(ram));
        lenient().when(productFeignClient.getProductsByCategory(7L)).thenReturn(List.of(ram));
        lenient().when(productFeignClient.getProductsByCategory(8L)).thenReturn(List.of(ram));

        List<Recommendation> savedList = List.of(
                createRec(3L, "MISSING_COMPONENT", 201L, "Te falta RAM: Corsair Vengeance (Corsair) - $150"),
                createRec(4L, "MISSING_COMPONENT", 201L, "Te falta Motherboard: Corsair Vengeance (Corsair) - $150"),
                createRec(5L, "MISSING_COMPONENT", 201L, "Te falta Storage: Corsair Vengeance (Corsair) - $150"),
                createRec(6L, "MISSING_COMPONENT", 201L, "Te falta PSU: Corsair Vengeance (Corsair) - $150"),
                createRec(7L, "MISSING_COMPONENT", 201L, "Te falta Case: Corsair Vengeance (Corsair) - $150"),
                createRec(8L, "MISSING_COMPONENT", 201L, "Te falta Cooling: Corsair Vengeance (Corsair) - $150")
        );
        when(recommendationRepository.saveAll(any())).thenReturn(savedList);

        List<RecommendationResponse> result = service.generate(new GenerateRecommendationsRequest(BUILD_ID));

        assertEquals(6, result.size());
        assertTrue(result.stream().allMatch(r -> "MISSING_COMPONENT".equals(r.ruleApplied())));

        verify(notificationFeignClient).sendNotification(notificationCaptor.capture());
        NotificationClientRequest sent = notificationCaptor.getValue();
        assertEquals(USER_ID, sent.userId());
        assertEquals("RECOMMENDATION", sent.type());
        assertEquals("INFO", sent.status());
        assertTrue(sent.content().contains("6"));
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
    @DisplayName("generate() — todas las categorías llenas (1 y 2), sin recomendaciones")
    void generate_allCategoriesFilled_shouldReturnEmpty() {
        when(buildFeignClient.getBuildById(BUILD_ID)).thenReturn(build);
        when(compatibilityFeignClient.checkCompatibility(any()))
                .thenReturn(new CompatibilityCheckResponse(1L, BUILD_ID, true, "All compatible", LocalDateTime.now()));
        when(productFeignClient.getProductById(100L)).thenReturn(currentGpu);
        when(productFeignClient.getProductById(101L)).thenReturn(currentCpu);

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
