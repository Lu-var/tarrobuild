package cl.tarrobuild.build.service;

import cl.tarrobuild.build.client.CompatibilityFeignClient;
import cl.tarrobuild.build.client.NotificationFeignClient;
import cl.tarrobuild.build.client.ProductFeignClient;
import cl.tarrobuild.build.dto.*;
import cl.tarrobuild.build.model.Build;
import cl.tarrobuild.build.model.BuildItem;
import cl.tarrobuild.build.model.BuildStatus;
import cl.tarrobuild.build.repository.BuildItemRepository;
import cl.tarrobuild.build.repository.BuildRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class BuildService {

    private final BuildRepository buildRepository;
    private final BuildItemRepository buildItemRepository;
    private final ProductFeignClient productFeignClient;
    private final CompatibilityFeignClient compatibilityFeignClient;
    private final NotificationFeignClient notificationFeignClient;

    public BuildService(BuildRepository buildRepository, BuildItemRepository buildItemRepository,
                        ProductFeignClient productFeignClient,
                        CompatibilityFeignClient compatibilityFeignClient,
                        NotificationFeignClient notificationFeignClient) {
        this.buildRepository = buildRepository;
        this.buildItemRepository = buildItemRepository;
        this.productFeignClient = productFeignClient;
        this.compatibilityFeignClient = compatibilityFeignClient;
        this.notificationFeignClient = notificationFeignClient;
    }

    public BuildResponse getBuildById(Long id) {
        log.info("Getting build by id: {}", id);
        return buildRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Build with ID " + id + " not found"));
    }

    public List<BuildResponse> getAllBuilds() {
        log.info("Getting all builds");
        return buildRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BuildResponse> getBuildsByUserId(Long userId) {
        log.info("Getting builds for userId: {}", userId);
        return buildRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public BuildResponse getBuildByIdAndUserId(Long buildId, Long userId) {
        log.info("Getting build by id: {} and userId: {}", buildId, userId);
        return buildRepository.findByIdAndUserId(buildId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Build with ID " + buildId + " not found for user " + userId));
    }

    public BuildResponse createBuild(BuildRequest request) {
        log.info("Creating build \"{}\" userId: {}", request.name(), request.userId());
        Build build = new Build();
        build.setUserId(request.userId());
        build.setName(request.name());

        Build saved = buildRepository.save(build);
        sendNotification(build.getUserId(), "BUILD_CREATED",
                "Build \"" + build.getName() + "\" created successfully", "SUCCESS");
        return toResponse(saved);
    }

    public BuildResponse updateBuildStatus(Long id, BuildStatus status) {
        log.info("Updating status for build: {}", id);
        Build targetBuild = buildRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Build with ID " + id + " not found"));
        targetBuild.setStatus(status);

        Build saved = buildRepository.save(targetBuild);
        sendNotification(targetBuild.getUserId(), "BUILD_STATUS",
                "Build \"" + targetBuild.getName() + "\" status changed to " + status, "INFO");
        return toResponse(saved);
    }

    public BuildResponse updateBuild(Long id, BuildRequest request) {
        log.info("Updating build id: {} with name: \"{}\" from userId: {}", id, request.name(), request.userId());
        return buildRepository.findById(id)
                .map(build -> {
                    build.setUserId(request.userId());
                    build.setName(request.name());

                    Build saved = buildRepository.save(build);
                    return toResponse(saved);
                })
                .orElseThrow(() -> new EntityNotFoundException("Build with ID " + id + " not found"));
    }

    public boolean deleteBuild(Long id) {
        if (!buildRepository.existsById(id)) {
            log.info("Build with id: {} not found for deletion", id);
            return false;
        }
        buildRepository.deleteById(id);
        log.info("Build with id: {} deleted successfully", id);
        return true;
    }

    public List<BuildItemResponse> getItemsByBuildId(Long buildId) {
        log.info("Getting items for buildId: {}", buildId);
        buildRepository.findById(buildId)
                .orElseThrow(() -> new EntityNotFoundException("Build with ID " + buildId + " not found"));

        return buildItemRepository.findByBuild_Id(buildId)
                .stream()
                .map(this::toItemResponse)
                .toList();
    }

    public BuildItemResponse getItemByIdAndBuildId(Long itemId, Long buildId) {
        log.info("Getting item with ID {} for buildId: {}", itemId, buildId);
        return buildItemRepository.findByIdAndBuild_Id(itemId, buildId)
                .map(this::toItemResponse)
                .orElseThrow(() -> new EntityNotFoundException("Item with ID " + itemId + " not found for build " + buildId));
    }

    public BuildItemResponse createItem(Long buildId, BuildItemRequest request) {
        log.info("Creating item for buildId: {}", buildId);
        Build targetBuild = buildRepository.findById(buildId)
                .orElseThrow(() -> new EntityNotFoundException("Build with ID " + buildId + " not found"));

        try {
            ProductClientResponse product = productFeignClient.getProductById(request.productId());
            if (product == null) {
                log.warn("Product with ID {} unavailable (product-service down)", request.productId());
                throw new IllegalArgumentException("Product service is currently unavailable");
            }
            if (!product.isActive()) {
                throw new IllegalArgumentException("Product with ID " + request.productId() + " is not active");
            }
        } catch (FeignException.NotFound e) {
            log.warn("Product with ID {} not found in product-service", request.productId());
            throw new EntityNotFoundException("Product with ID " + request.productId() + " not found");
        }

        log.info("Creating item with productId: {} and quantity: {}", request.productId(), request.quantity());
        BuildItem newItem = new BuildItem();
        newItem.setProductId(request.productId());
        newItem.setQuantity(request.quantity());
        newItem.setBuild(targetBuild);

        BuildItem saved = buildItemRepository.save(newItem);

        triggerCompatibilityCheck(buildId);

        return toItemResponse(saved);
    }

    public BuildItemResponse updateItem(Long buildId, Long itemId, BuildItemRequest request) {
        log.info("Updating item for buildId: {}", buildId);
        buildRepository.findById(buildId)
                .orElseThrow(() -> new EntityNotFoundException("Build with ID " + buildId + " not found"));
        BuildItem targetItem = buildItemRepository.findByIdAndBuild_Id(itemId, buildId)
                .orElseThrow(() -> new EntityNotFoundException("Item with ID " + itemId + " not found"));

        targetItem.setProductId(request.productId());
        targetItem.setQuantity(request.quantity());
        BuildItem saved = buildItemRepository.save(targetItem);

        triggerCompatibilityCheck(buildId);

        return toItemResponse(saved);
    }

    public void deleteItem(Long itemId, Long buildId) {
        log.info("Deleting item with ID {} for buildId: {}", itemId, buildId);
        BuildItem item = buildItemRepository.findByIdAndBuild_Id(itemId, buildId)
                .orElseThrow(() -> new EntityNotFoundException("Item with ID " + itemId + " not found for build " + buildId));
        buildItemRepository.delete(item);

        triggerCompatibilityCheck(buildId);
    }

    private void triggerCompatibilityCheck(Long buildId) {
        try {
            List<BuildItem> items = buildItemRepository.findByBuild_Id(buildId);
            if (items.size() < 2) {
                log.info("Build {} has fewer than 2 items, skipping compatibility check", buildId);
                return;
            }
            List<Long> productIds = items.stream().map(BuildItem::getProductId).toList();
            CompatibilityClientRequest request = new CompatibilityClientRequest(buildId, productIds);
            CompatibilityClientResponse result = compatibilityFeignClient.checkCompatibility(request);
            if (result != null) {
                if (Boolean.TRUE.equals(result.result())) {
                    log.info("Compatibility check PASSED for build {}", buildId);
                } else {
                    log.warn("Compatibility check FAILED for build {}: {}", buildId, result.details());
                }
            }
        } catch (Exception e) {
            log.warn("Could not run compatibility check for build {}: {}", buildId, e.getMessage());
        }
    }

    private void sendNotification(Long userId, String type, String content, String status) {
        try {
            notificationFeignClient.sendNotification(new NotificationClientRequest(userId, type, content, status));
            log.info("Notification sent: {} for user {}", type, userId);
        } catch (Exception e) {
            log.warn("Failed to send notification: {}", e.getMessage());
        }
    }

    private BuildResponse toResponse(Build build) {
        return new BuildResponse(
                build.getId(),
                build.getUserId(),
                build.getName(),
                build.getStatus().name(),
                build.getCreatedAt(),
                toItemResponseList(build.getItems())
        );
    }

    private List<BuildItemResponse> toItemResponseList(List<BuildItem> items) {
        return items.stream()
                .map(this::toItemResponse)
                .toList();
    }

    private BuildItemResponse toItemResponse(BuildItem item) {
        return new BuildItemResponse(
                item.getId(),
                Optional.ofNullable(item.getBuild())
                        .map(Build::getId)
                        .orElse(null),
                item.getProductId(),
                item.getQuantity()
        );
    }
}
