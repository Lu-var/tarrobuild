package cl.tarrobuild.estimate.service;

import cl.tarrobuild.estimate.client.BuildRestClient;
import cl.tarrobuild.estimate.client.NotificationRestClient;
import cl.tarrobuild.estimate.client.ProductRestClient;
import cl.tarrobuild.estimate.dto.BuildClientResponse;
import cl.tarrobuild.estimate.dto.EstimateRequest;
import cl.tarrobuild.estimate.dto.EstimateResponse;
import cl.tarrobuild.estimate.dto.NotificationClientRequest;
import cl.tarrobuild.estimate.model.Estimate;
import cl.tarrobuild.estimate.repository.EstimateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EstimateService {

    private final EstimateRepository estimateRepository;
    private final BuildRestClient buildRestClient;
    private final ProductRestClient productRestClient;
    private final NotificationRestClient notificationRestClient;

    public EstimateService(EstimateRepository estimateRepository,
                           BuildRestClient buildRestClient,
                           ProductRestClient productRestClient,
                           NotificationRestClient notificationRestClient) {
        this.estimateRepository = estimateRepository;
        this.buildRestClient = buildRestClient;
        this.productRestClient = productRestClient;
        this.notificationRestClient = notificationRestClient;
    }

    public EstimateResponse calculate(EstimateRequest request) {
        log.info("Calculating estimate for buildId: {}", request.buildId());

        BuildClientResponse build = buildRestClient.getBuildById(request.buildId());
        log.info("Build found: id={}, items={}", build.id(), build.items().size());

        int totalPrice = build.items().stream()
                .mapToInt(item -> {
                    var product = productRestClient.getProductById(item.productId());
                    log.debug("Product {} price: {} x {}", item.productId(), product.price(), item.quantity());
                    return product.price() * item.quantity();
                })
                .sum();
        log.info("Total price calculated: {}", totalPrice);

        Estimate estimate = new Estimate();
        estimate.setBuildId(request.buildId());
        estimate.setTotalPrice(totalPrice);
        estimate.setCurrency(request.currency() != null ? request.currency() : "USD");

        Estimate saved = estimateRepository.save(estimate);
        log.info("Estimate created with id: {} for buildId: {}", saved.getId(), saved.getBuildId());

        notificationRestClient.sendNotification(new NotificationClientRequest(
                build.userId(),
                "ESTIMATE",
                "Your build \"" + build.name() + "\" estimate is $" + totalPrice,
                "INFO"
        ));

        return toResponse(saved);
    }

    public List<EstimateResponse> getEstimatesByBuildId(Long buildId) {
        log.info("Getting estimates for buildId: {}", buildId);
        return estimateRepository.findByBuildId(buildId).stream()
                .map(this::toResponse)
                .toList();
    }

    public EstimateResponse getLatestEstimateByBuildId(Long buildId) {
        log.info("Getting latest estimate for buildId: {}", buildId);
        return estimateRepository.findTopByBuildIdOrderByCreatedAtDesc(buildId)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No estimate found for build ID " + buildId));
    }

    public EstimateResponse getEstimateById(Long id) {
        log.info("Getting estimate by id: {}", id);
        return estimateRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Estimate with ID " + id + " not found"));
    }

    private EstimateResponse toResponse(Estimate estimate) {
        return new EstimateResponse(
                estimate.getId(),
                estimate.getBuildId(),
                estimate.getTotalPrice(),
                estimate.getCurrency(),
                estimate.getCreatedAt()
        );
    }
}
