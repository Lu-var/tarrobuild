package cl.tarrobuild.compatibility.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "compatibility_rules")
@Getter
@Setter
@NoArgsConstructor
public class CompatibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sourceCategory;

    @Column(nullable = false)
    private String sourceAttributeName;

    @Column(nullable = false)
    private String operator;

    @Column(nullable = false)
    private String targetCategory;

    @Column(nullable = false)
    private String targetAttributeName;

    @Column(nullable = false, length = 500)
    private String incompatibilityReason;
}
