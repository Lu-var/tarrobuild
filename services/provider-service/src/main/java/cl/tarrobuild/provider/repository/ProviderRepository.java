package cl.tarrobuild.provider.repository;

import cl.tarrobuild.provider.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    List<Provider> findByIsActiveTrue();
}
