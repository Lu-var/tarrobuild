package cl.tarrobuild.compatibility.repository;

import cl.tarrobuild.compatibility.model.CompatibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompatibilityRuleRepository extends JpaRepository<CompatibilityRule, Long> {

    List<CompatibilityRule> findBySourceCategory(String sourceCategory);

    List<CompatibilityRule> findByTargetCategory(String targetCategory);
}
