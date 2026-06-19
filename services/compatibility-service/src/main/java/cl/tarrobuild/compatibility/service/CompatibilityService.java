package cl.tarrobuild.compatibility.service;

import cl.tarrobuild.compatibility.client.ProductClient;
import cl.tarrobuild.compatibility.dto.CompatibilityCheckRequest;
import cl.tarrobuild.compatibility.dto.CompatibilityCheckResponse;
import cl.tarrobuild.compatibility.dto.CompatibilityRuleRequest;
import cl.tarrobuild.compatibility.dto.CompatibilityRuleResponse;
import cl.tarrobuild.compatibility.dto.ProductDTO;
import cl.tarrobuild.compatibility.model.CompatibilityCheck;
import cl.tarrobuild.compatibility.model.CompatibilityRule;
import cl.tarrobuild.compatibility.repository.CompatibilityCheckRepository;
import cl.tarrobuild.compatibility.repository.CompatibilityRuleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CompatibilityService {

    private final CompatibilityRuleRepository ruleRepository;
    private final CompatibilityCheckRepository checkRepository;
    private final ProductClient productClient;

    public CompatibilityService(CompatibilityRuleRepository ruleRepository,
                                CompatibilityCheckRepository checkRepository,
                                ProductClient productClient) {
        this.ruleRepository = ruleRepository;
        this.checkRepository = checkRepository;
        this.productClient = productClient;
    }

    public CompatibilityCheckResponse check(CompatibilityCheckRequest request) {
        log.info("Running compatibility check for buildId: {} with {} products",
                request.buildId(), request.productIds().size());

        List<CompatibilityRule> rules = ruleRepository.findAll();
        if (rules.isEmpty()) {
            log.warn("No compatibility rules defined — marking as compatible by default");
            CompatibilityCheck check = new CompatibilityCheck();
            check.setBuildId(request.buildId());
            check.setProductIds(request.productIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
            check.setResult(true);
            check.setDetails("No rules defined — no incompatibilities found.");
            CompatibilityCheck saved = checkRepository.save(check);
            return toResponse(saved);
        }

        StringBuilder detailsBuilder = new StringBuilder();
        boolean allCompatible = true;

        for (CompatibilityRule rule : rules) {
            log.debug("Evaluating rule: {} {} {} -> {} {}",
                    rule.getSourceCategory(), rule.getSourceAttributeName(),
                    rule.getOperator(), rule.getTargetCategory(), rule.getTargetAttributeName());

            boolean ruleResult = evaluateRule(rule, request.productIds());
            if (!ruleResult) {
                allCompatible = false;
                detailsBuilder.append("- ").append(rule.getIncompatibilityReason()).append("\n");
                log.info("Rule failed: {}", rule.getIncompatibilityReason());
            }
        }

        String details = allCompatible
                ? "All components are compatible."
                : "Incompatibilities found:\n" + detailsBuilder.toString().trim();

        CompatibilityCheck check = new CompatibilityCheck();
        check.setBuildId(request.buildId());
        check.setProductIds(request.productIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        check.setResult(allCompatible);
        check.setDetails(details);

        CompatibilityCheck saved = checkRepository.save(check);
        return toResponse(saved);
    }

    private boolean evaluateRule(CompatibilityRule rule, List<Long> productIds) {
        log.debug("Consumiendo cliente REST remoto para validar reglas distribuidas");

        List<ProductDTO> products = productIds.stream()
                .map(id -> {
                    try {
                        return productClient.getProductById(id);
                    } catch (Exception e) {
                        log.error("Error de comunicación remota con product-service para ID: {}", id, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        ProductDTO sourceProduct = products.stream()
                .filter(p -> p.categoryName() != null && p.categoryName().equalsIgnoreCase(rule.getSourceCategory()))
                .findFirst()
                .orElse(null);

        ProductDTO targetProduct = products.stream()
                .filter(p -> p.categoryName() != null && p.categoryName().equalsIgnoreCase(rule.getTargetCategory()))
                .findFirst()
                .orElse(null);

        if (sourceProduct == null || targetProduct == null) {
            return true;
        }

        if ("socketType".equalsIgnoreCase(rule.getSourceAttributeName()) && "EQUALS".equalsIgnoreCase(rule.getOperator())) {
            if (sourceProduct.socketType() == null || targetProduct.socketType() == null) {
                return false;
            }
            return sourceProduct.socketType().equalsIgnoreCase(targetProduct.socketType());
        }

        return true;
    }

    public CompatibilityCheckResponse getLatestCheckByBuildId(Long buildId) {
        log.info("Getting latest compatibility check for buildId: {}", buildId);
        return checkRepository.findTopByBuildIdOrderByCreatedAtDesc(buildId)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No compatibility check found for build ID " + buildId));
    }

    public CompatibilityCheckResponse getCheckById(Long id) {
        log.info("Getting compatibility check by id: {}", id);
        return checkRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Compatibility check with ID " + id + " not found"));
    }

    public CompatibilityRuleResponse createRule(CompatibilityRuleRequest request) {
        log.info("Creating compatibility rule: {} {} {} -> {} {}",
                request.sourceCategory(), request.sourceAttributeName(), request.operator(),
                request.targetCategory(), request.targetAttributeName());

        CompatibilityRule rule = new CompatibilityRule();
        rule.setSourceCategory(request.sourceCategory());
        rule.setSourceAttributeName(request.sourceAttributeName());
        rule.setOperator(request.operator());
        rule.setTargetCategory(request.targetCategory());
        rule.setTargetAttributeName(request.targetAttributeName());
        rule.setIncompatibilityReason(request.incompatibilityReason());

        CompatibilityRule saved = ruleRepository.save(rule);
        log.info("Compatibility rule created with id: {}", saved.getId());
        return toRuleResponse(saved);
    }

    public List<CompatibilityRuleResponse> getAllRules() {
        log.info("Getting all compatibility rules");
        return ruleRepository.findAll().stream()
                .map(this::toRuleResponse)
                .toList();
    }

    public CompatibilityRuleResponse getRuleById(Long id) {
        log.info("Getting compatibility rule by id: {}", id);
        return ruleRepository.findById(id)
                .map(this::toRuleResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Compatibility rule with ID " + id + " not found"));
    }

    public CompatibilityRuleResponse updateRule(Long id, CompatibilityRuleRequest request) {
        log.info("Updating compatibility rule id: {}", id);
        return ruleRepository.findById(id)
                .map(rule -> {
                    rule.setSourceCategory(request.sourceCategory());
                    rule.setSourceAttributeName(request.sourceAttributeName());
                    rule.setOperator(request.operator());
                    rule.setTargetCategory(request.targetCategory());
                    rule.setTargetAttributeName(request.targetAttributeName());
                    rule.setIncompatibilityReason(request.incompatibilityReason());
                    CompatibilityRule saved = ruleRepository.save(rule);
                    log.info("Compatibility rule with id: {} updated", id);
                    return toRuleResponse(saved);
                })
                .orElseThrow(() -> new EntityNotFoundException(
                        "Compatibility rule with ID " + id + " not found"));
    }

    public boolean deleteRule(Long id) {
        if (!ruleRepository.existsById(id)) {
            log.info("Compatibility rule with id: {} not found for deletion", id);
            return false;
        }
        ruleRepository.deleteById(id);
        log.info("Compatibility rule with id: {} deleted", id);
        return true;
    }

    private CompatibilityCheckResponse toResponse(CompatibilityCheck check) {
        return new CompatibilityCheckResponse(
                check.getId(),
                check.getBuildId(),
                check.getResult(),
                check.getDetails(),
                check.getCreatedAt()
        );
    }

    private CompatibilityRuleResponse toRuleResponse(CompatibilityRule rule) {
        return new CompatibilityRuleResponse(
                rule.getId(),
                rule.getSourceCategory(),
                rule.getSourceAttributeName(),
                rule.getOperator(),
                rule.getTargetCategory(),
                rule.getTargetAttributeName(),
                rule.getIncompatibilityReason()
        );
    }
}