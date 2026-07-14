package cl.tarrobuild.build.service;

import cl.tarrobuild.build.dto.FavoriteResponse;
import cl.tarrobuild.build.model.Build;
import cl.tarrobuild.build.model.Favorite;
import cl.tarrobuild.build.repository.BuildRepository;
import cl.tarrobuild.build.repository.FavoriteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final BuildRepository buildRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, BuildRepository buildRepository) {
        this.favoriteRepository = favoriteRepository;
        this.buildRepository = buildRepository;
    }

    @Transactional
    public FavoriteResponse toggleFavorite(Long userId, Long buildId) {
        buildRepository.findById(buildId)
                .orElseThrow(() -> new EntityNotFoundException("Build with ID " + buildId + " not found"));

        var existing = favoriteRepository.findByUserIdAndBuildId(userId, buildId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            log.info("Removed favorite for build {} by user {}", buildId, userId);
            return new FavoriteResponse(existing.get().getId(), buildId,
                    buildRepository.findById(buildId).map(Build::getName).orElse(""), "removed");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setBuildId(buildId);
        Favorite saved = favoriteRepository.save(favorite);
        log.info("Added favorite for build {} by user {}", buildId, userId);

        String buildName = buildRepository.findById(buildId).map(Build::getName).orElse("");
        return new FavoriteResponse(saved.getId(), buildId, buildName,
                saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : "");
    }

    public List<FavoriteResponse> getFavoritesByUserId(Long userId) {
        log.info("Getting favorites for userId: {}", userId);
        return favoriteRepository.findByUserId(userId)
                .stream()
                .map(f -> {
                    String buildName = buildRepository.findById(f.getBuildId())
                            .map(Build::getName).orElse("Unknown");
                    return new FavoriteResponse(f.getId(), f.getBuildId(), buildName,
                            f.getCreatedAt() != null ? f.getCreatedAt().toString() : null);
                })
                .toList();
    }
}
