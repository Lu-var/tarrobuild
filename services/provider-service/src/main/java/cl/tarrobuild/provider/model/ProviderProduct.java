package cl.tarrobuild.provider.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "provider_products")
@Getter
@Setter
@NoArgsConstructor
public class ProviderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "external_reference")
    private String externalReference;
}
