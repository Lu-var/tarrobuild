package cl.tarrobuild.product.repository;

import cl.tarrobuild.product.model.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {

     List<ProductAttribute> findByProductId(Long productId);
     Optional<ProductAttribute> findByIdAndProductId(Long id, Long productId);
     Optional<ProductAttribute> findByAttributeNameAndProductId(String attributeName, Long productId);
     boolean existsByAttributeNameAndProductId(String attributeName, Long productId);
}
