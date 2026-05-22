package cl.tarrobuild.product.repository;

import cl.tarrobuild.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByIsActiveTrue();

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByBrandIgnoreCase(String brand);

    List<Product> findByMsrpBetween(Integer minMsrp, Integer maxMsrp);
}