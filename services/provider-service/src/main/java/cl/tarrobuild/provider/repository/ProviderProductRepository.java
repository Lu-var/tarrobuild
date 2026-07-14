package cl.tarrobuild.provider.repository;

import cl.tarrobuild.provider.model.ProviderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderProductRepository extends JpaRepository<ProviderProduct, Long> {

    List<ProviderProduct> findByProvider_Id(Long providerId);

    List<ProviderProduct> findByProductId(Long productId);

    Optional<ProviderProduct> findByIdAndProvider_Id(Long id, Long providerId);
}
