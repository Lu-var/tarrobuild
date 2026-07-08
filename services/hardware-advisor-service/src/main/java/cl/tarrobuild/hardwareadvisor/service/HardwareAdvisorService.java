package cl.tarrobuild.hardwareadvisor.service;

import cl.tarrobuild.hardwareadvisor.client.BuildFeignClient;
import cl.tarrobuild.hardwareadvisor.client.CompatibilityFeignClient;
import cl.tarrobuild.hardwareadvisor.client.NotificationFeignClient;
import cl.tarrobuild.hardwareadvisor.client.ProductFeignClient;
import cl.tarrobuild.hardwareadvisor.dto.*;
import cl.tarrobuild.hardwareadvisor.model.Recommendation;
import cl.tarrobuild.hardwareadvisor.repository.RecommendationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class HardwareAdvisorService {

    private final RecommendationRepository recommendationRepository;
    private final BuildFeignClient buildFeignClient;
    private final ProductFeignClient productFeignClient;
    private final CompatibilityFeignClient compatibilityFeignClient;
    private final NotificationFeignClient notificationFeignClient;

    public HardwareAdvisorService(RecommendationRepository recommendationRepository,
                                  BuildFeignClient buildFeignClient,
                                  ProductFeignClient productFeignClient,
                                  CompatibilityFeignClient compatibilityFeignClient,
                                  NotificationFeignClient notificationFeignClient) {
        this.recommendationRepository = recommendationRepository;
        this.buildFeignClient = buildFeignClient;
        this.productFeignClient = productFeignClient;
        this.compatibilityFeignClient = compatibilityFeignClient;
        this.notificationFeignClient = notificationFeignClient;
    }

    public List<RecommendationResponse> generate(GenerateRecommendationsRequest request) {
        log.info("Generating recommendations for buildId: {}", request.buildId());

        List<Recommendation> recommendations = new ArrayList<>();

        // 1. Fetch build details
        BuildClientResponse build = buildFeignClient.getBuildById(request.buildId());
        if (build == null || build.items() == null || build.items().isEmpty()) {
            log.warn("Build {} not found or has no items", request.buildId());
            return List.of();
        }

        // 2. Extract product IDs for compatibility check
        List<Long> productIds = build.items().stream()
                .map(BuildItemClientResponse::productId)
                .toList();

        // 3. Run compatibility check
        CompatibilityCheckRequest compatRequest = new CompatibilityCheckRequest(request.buildId(), productIds);
        CompatibilityCheckResponse compatResult = compatibilityFeignClient.checkCompatibility(compatRequest);

        if (compatResult != null && Boolean.FALSE.equals(compatResult.result())) {
            recommendations.add(createRecommendation(
                    request.buildId(),
                    "INCOMPATIBILITY",
                    null,
                    "Build has compatibility issues: " + compatResult.details()
            ));
        }

        // 4. For each product, look for upgrades
        for (BuildItemClientResponse item : build.items()) {
            ProductClientResponse currentProduct = productFeignClient.getProductById(item.productId());
            if (currentProduct == null) continue;

            List<ProductClientResponse> alternatives = productFeignClient.getProductsByCategory(currentProduct.categoryId());
            for (ProductClientResponse alt : alternatives) {
                if (alt.id().equals(currentProduct.id()) || !alt.isActive()) continue;
                if (alt.msrp() > currentProduct.msrp()) {
                    recommendations.add(createRecommendation(
                            request.buildId(),
                            "UPGRADE",
                            alt.id(),
                            String.format("Consider upgrading %s (%s %s) to %s (%s %s) for +$%d",
                                    currentProduct.name(), currentProduct.brand(), currentProduct.model(),
                                    alt.name(), alt.brand(), alt.model(),
                                    alt.msrp() - currentProduct.msrp())
                    ));
                }
            }
        }

        // 5. Save
        List<Recommendation> saved = recommendationRepository.saveAll(recommendations);

        // 6. Notify user
        if (!saved.isEmpty()) {
            notificationFeignClient.sendNotification(new NotificationClientRequest(
                    build.userId(),
                    "RECOMMENDATION",
                    String.format("Se generaron %d recomendaciones para tu build '%s'", saved.size(), build.name()),
                    "INFO"
            ));
        }

        log.info("Generated {} recommendations for buildId: {}", saved.size(), request.buildId());
        return saved.stream().map(this::toResponse).toList();
    }

    public List<RecommendationResponse> getRecommendationsByBuildId(Long buildId) {
        log.info("Getting recommendations for buildId: {}", buildId);
        return recommendationRepository.findByBuildId(buildId).stream()
                .map(this::toResponse)
                .toList();
    }

    public RecommendationResponse getRecommendationById(Long id) {
        log.info("Getting recommendation by id: {}", id);
        return recommendationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Recommendation with ID " + id + " not found"));
    }

    private Recommendation createRecommendation(Long buildId, String ruleApplied, Long suggestedProductId, String reason) {
        Recommendation rec = new Recommendation();
        rec.setBuildId(buildId);
        rec.setRuleApplied(ruleApplied);
        rec.setSuggestedProductId(suggestedProductId);
        rec.setReason(reason);
        return rec;
    }

    private RecommendationResponse toResponse(Recommendation recommendation) {
        return new RecommendationResponse(
                recommendation.getId(),
                recommendation.getBuildId(),
                recommendation.getRuleApplied(),
                recommendation.getSuggestedProductId(),
                recommendation.getReason(),
                recommendation.getCreatedAt()
        );
    }
}
