package cl.tarrobuild.hardwareadvisor.service;

import cl.tarrobuild.hardwareadvisor.dto.GenerateRecommendationsRequest;
import cl.tarrobuild.hardwareadvisor.dto.RecommendationResponse;
import cl.tarrobuild.hardwareadvisor.model.Recommendation;
import cl.tarrobuild.hardwareadvisor.repository.RecommendationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class HardwareAdvisorService {

    private final RecommendationRepository recommendationRepository;

    public HardwareAdvisorService(RecommendationRepository recommendationRepository) {
        this.recommendationRepository = recommendationRepository;
    }

    public List<RecommendationResponse> generate(GenerateRecommendationsRequest request) {
        log.info("Generating recommendations for buildId: {}", request.buildId());

        // Stub: no external calls yet. Returns empty list.
        log.info("No recommendations generated for buildId: {} (stub)", request.buildId());
        return List.of();
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
