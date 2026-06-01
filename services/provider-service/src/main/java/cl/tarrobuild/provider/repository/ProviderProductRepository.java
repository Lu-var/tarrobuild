package cl.tarrobuild.provider.repository;

import cl.tarrobuild.provider.model.ProviderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderProductRepository extends JpaRepository<ProviderProduct, Long> {

    List<ProviderProduct> findByProviderId(Long providerId);

    List<ProviderProduct> findByProductId(Long productId);

    Optional<ProviderProduct> findByIdAndProviderId(Long id, Long providerId);
}
