package cl.tarrobuild.hardwareadvisor.repository;

import cl.tarrobuild.hardwareadvisor.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByBuildId(Long buildId);
}
