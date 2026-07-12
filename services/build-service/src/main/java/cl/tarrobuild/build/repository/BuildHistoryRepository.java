package cl.tarrobuild.build.repository;

import cl.tarrobuild.build.model.BuildHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildHistoryRepository extends JpaRepository<BuildHistory, Long> {
    List<BuildHistory> findByBuildIdOrderByChangedAtDesc(Long buildId);
}
