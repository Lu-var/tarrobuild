package cl.tarrobuild.compatibility.repository;

import cl.tarrobuild.compatibility.model.CompatibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompatibilityRuleRepository extends JpaRepository<CompatibilityRule, Long> {
}
