package cl.tarrobuild.build.service;

import cl.tarrobuild.build.dto.BuildHistoryResponse;
import cl.tarrobuild.build.model.Build;
import cl.tarrobuild.build.model.BuildHistory;
import cl.tarrobuild.build.repository.BuildHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class BuildHistoryService {

    private final BuildHistoryRepository historyRepository;

    public BuildHistoryService(BuildHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public void saveSnapshot(Build build) {
        String data = String.format(
                "{\"name\":\"%s\",\"status\":\"%s\",\"userId\":%d,\"items\":%d}",
                build.getName(), build.getStatus(), build.getUserId(),
                build.getItems() != null ? build.getItems().size() : 0
        );
        BuildHistory history = new BuildHistory();
        history.setBuildId(build.getId());
        history.setData(data);
        historyRepository.save(history);
        log.info("Build history snapshot saved for build {}", build.getId());
    }

    public List<BuildHistoryResponse> getHistoryByBuildId(Long buildId) {
        log.info("Getting history for buildId: {}", buildId);
        return historyRepository.findByBuildIdOrderByChangedAtDesc(buildId)
                .stream()
                .map(h -> new BuildHistoryResponse(
                        h.getId(), h.getBuildId(), h.getData(),
                        h.getChangedAt() != null ? h.getChangedAt().toString() : null
                ))
                .toList();
    }
}
