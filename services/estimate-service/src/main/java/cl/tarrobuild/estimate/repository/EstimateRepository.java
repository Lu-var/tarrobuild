package cl.tarrobuild.estimate.repository;

import cl.tarrobuild.estimate.model.Estimate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstimateRepository extends JpaRepository<Estimate, Long> {

    List<Estimate> findByBuildId(Long buildId);

    Optional<Estimate> findTopByBuildIdOrderByCreatedAtDesc(Long buildId);
}
