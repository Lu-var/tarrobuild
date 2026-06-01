package cl.tarrobuild.compatibility.repository;

import cl.tarrobuild.compatibility.model.CompatibilityCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompatibilityCheckRepository extends JpaRepository<CompatibilityCheck, Long> {

    List<CompatibilityCheck> findByBuildId(Long buildId);

    Optional<CompatibilityCheck> findTopByBuildIdOrderByCreatedAtDesc(Long buildId);
}
