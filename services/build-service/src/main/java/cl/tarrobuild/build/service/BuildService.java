package cl.tarrobuild.build.service;

import cl.tarrobuild.build.dto.BuildItemRequest;
import cl.tarrobuild.build.dto.BuildItemResponse;
import cl.tarrobuild.build.dto.BuildRequest;
import cl.tarrobuild.build.dto.BuildResponse;
import cl.tarrobuild.build.model.Build;
import cl.tarrobuild.build.model.BuildItem;
import cl.tarrobuild.build.model.BuildStatus;
import cl.tarrobuild.build.repository.BuildItemRepository;
import cl.tarrobuild.build.repository.BuildRepository;
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

    public BuildService(BuildRepository buildRepository, BuildItemRepository buildItemRepository) {
        this.buildRepository = buildRepository;
        this.buildItemRepository = buildItemRepository;
    }

    public List<BuildResponse> getBuilds() {
        log.info("Getting all builds");
        return buildRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public BuildResponse getBuildById(Long id) {
        log.info("Getting build by id: {}", id);
        return buildRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Build with ID " + id + " not found"));
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
        return toResponse(saved);
    }

    public BuildResponse updateBuildStatus(Long id, BuildStatus status) {
        log.info("Updating status for build: {}", id);
        Build targetBuild = buildRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Build with ID " + id + " not found"));
        targetBuild.setStatus(status);

        Build saved = buildRepository.save(targetBuild);
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

        log.info("Creating item with productId: {} and quantity: {}", request.productId(), request.quantity());
        BuildItem newItem = new BuildItem();
        newItem.setProductId(request.productId());
        newItem.setQuantity(request.quantity());
        newItem.setBuild(targetBuild);

        BuildItem saved = buildItemRepository.save(newItem);
        return toItemResponse(saved);
    }

    public BuildItemResponse updateItem(Long buildId, Long itemId, BuildItemRequest request) {
        log.info("Updating item for buildId: {}", buildId);
        Build targetBuild = buildRepository.findById(buildId)
                .orElseThrow(() -> new EntityNotFoundException("Build with ID " + buildId + " not found"));
        BuildItem targetItem = buildItemRepository.findByIdAndBuild_Id(itemId, buildId)
                .orElseThrow(() -> new EntityNotFoundException("Item with ID " + itemId + " not found"));
        targetItem.setId(request.productId());
        targetItem.setQuantity(request.quantity());
        BuildItem saved = buildItemRepository.save(targetItem);
        return toItemResponse(saved);
    }

    public void deleteItem(Long itemId, Long buildId) {
        log.info("Deleting item with ID {} for buildId: {}", itemId, buildId);
        BuildItem item = buildItemRepository.findByIdAndBuild_Id(itemId, buildId)
                .orElseThrow(() -> new EntityNotFoundException("Item with ID " + itemId + " not found for build " + buildId));
        buildItemRepository.delete(item);
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
