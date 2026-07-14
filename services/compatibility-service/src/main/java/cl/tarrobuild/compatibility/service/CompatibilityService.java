package cl.tarrobuild.compatibility.service;

import cl.tarrobuild.compatibility.client.CategoryRestClient;
import cl.tarrobuild.compatibility.client.ProductRestClient;
import cl.tarrobuild.compatibility.dto.AttributeDTO;
import cl.tarrobuild.compatibility.dto.CategoryClientResponse;
import cl.tarrobuild.compatibility.dto.CompatibilityCheckRequest;
import cl.tarrobuild.compatibility.dto.CompatibilityCheckResponse;
import cl.tarrobuild.compatibility.dto.CompatibilityRuleRequest;
import cl.tarrobuild.compatibility.dto.CompatibilityRuleResponse;
import cl.tarrobuild.compatibility.dto.ProductClientResponse;
import cl.tarrobuild.compatibility.dto.ProductDTO;
import cl.tarrobuild.compatibility.model.CompatibilityCheck;
import cl.tarrobuild.compatibility.model.CompatibilityRule;
import cl.tarrobuild.compatibility.repository.CompatibilityCheckRepository;
import cl.tarrobuild.compatibility.repository.CompatibilityRuleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CompatibilityService {

    private final CompatibilityRuleRepository ruleRepository;
    private final CompatibilityCheckRepository checkRepository;
    private final ProductRestClient productRestClient;
    private final CategoryRestClient categoryRestClient;

    public CompatibilityService(CompatibilityRuleRepository ruleRepository,
                                CompatibilityCheckRepository checkRepository,
                                ProductRestClient productRestClient,
                                CategoryRestClient categoryRestClient) {
        this.ruleRepository = ruleRepository;
        this.checkRepository = checkRepository;
        this.productRestClient = productRestClient;
        this.categoryRestClient = categoryRestClient;
    }

    public CompatibilityCheckResponse check(CompatibilityCheckRequest request) {
        log.info("Running compatibility check for buildId: {} with {} products",
                request.buildId(), request.productIds().size());

        List<CompatibilityRule> rules = ruleRepository.findAll();
        if (rules.isEmpty()) {
            log.warn("No compatibility rules defined — marking as compatible by default");
            CompatibilityCheck check = new CompatibilityCheck();
            if (request.buildId() != null) {
                check.setBuildId(request.buildId());
            }
            check.setProductIds(request.productIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
            check.setResult(true);
            check.setDetails("No rules defined — no incompatibilities found.");
            CompatibilityCheck saved = checkRepository.save(check);
            return toResponse(saved);
        }

        // Fetch all products once — shared across every rule evaluation
        List<ProductClientResponse> rawProducts = fetchProducts(request.productIds());

        // Resolve category names once per unique categoryId
        Map<Long, String> categoryNameById = resolveCategoryNames(rawProducts);

        // Map raw responses to ProductDTO (domain view with categoryName + generic attributes)
        List<ProductDTO> products = rawProducts.stream()
                .map(p -> toProductDTO(p, categoryNameById))
                .toList();

        StringBuilder detailsBuilder = new StringBuilder();
        boolean allCompatible = true;

        for (CompatibilityRule rule : rules) {
            log.debug("Evaluating rule: {} {} {} -> {} {}",
                    rule.getSourceCategory(), rule.getSourceAttributeName(),
                    rule.getOperator(), rule.getTargetCategory(), rule.getTargetAttributeName());

            boolean ruleResult = evaluateRule(rule, products);
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
        if (request.buildId() != null) {
            check.setBuildId(request.buildId());
        }
        check.setProductIds(request.productIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        check.setResult(allCompatible);
        check.setDetails(details);

        CompatibilityCheck saved = checkRepository.save(check);
        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Fetches each product from product-service via RestClient. Products that cannot be
     * retrieved are silently skipped so a single unreachable product does not abort
     * the entire check.
     */
    private List<ProductClientResponse> fetchProducts(List<Long> productIds) {
        List<ProductClientResponse> result = new ArrayList<>();
        for (Long id : productIds) {
            try {
                ProductClientResponse product = productRestClient.getProductById(id);
                if (product != null) {
                    result.add(product);
                }
            } catch (Exception e) {
                log.error("Error fetching product ID {} from product-service: {}", id, e.getMessage());
            }
        }
        return result;
    }

    /**
     * Calls category-service once per distinct categoryId found in the product list.
     * Missing or unreachable categories fall back to an empty string so the rule matcher
     * can still run (the rule will simply not find a matching product for that category).
     */
    private Map<Long, String> resolveCategoryNames(List<ProductClientResponse> products) {
        Map<Long, String> categoryNameById = new HashMap<>();
        for (ProductClientResponse p : products) {
            if (p.categoryId() == null || categoryNameById.containsKey(p.categoryId())) {
                continue;
            }
            CategoryClientResponse category = categoryRestClient.getCategoryById(p.categoryId());
            String name = (category != null) ? category.name() : "";
            categoryNameById.put(p.categoryId(), name);
        }
        return categoryNameById;
    }

    /**
     * Converts the raw RestClient response into the domain DTO used by the rule evaluator.
     */
    private ProductDTO toProductDTO(ProductClientResponse raw, Map<Long, String> categoryNameById) {
        String categoryName = categoryNameById.getOrDefault(raw.categoryId(), "");
        List<AttributeDTO> attributes = raw.attributes() == null
                ? List.of()
                : raw.attributes().stream()
                        .map(a -> new AttributeDTO(a.attributeName(), a.attributeValue()))
                        .toList();
        return new ProductDTO(raw.id(), raw.name(), categoryName, attributes);
    }

    /**
     * Evaluates a single compatibility rule against the pre-fetched product list.
     * Returns {@code true} (compatible) when the rule passes or when one of the
     * required products is not present in the build (rule is not applicable).
     */
    private boolean evaluateRule(CompatibilityRule rule, List<ProductDTO> products) {
        ProductDTO sourceProduct = products.stream()
                .filter(p -> p.categoryName() != null
                        && p.categoryName().equalsIgnoreCase(rule.getSourceCategory()))
                .findFirst()
                .orElse(null);

        ProductDTO targetProduct = products.stream()
                .filter(p -> p.categoryName() != null
                        && p.categoryName().equalsIgnoreCase(rule.getTargetCategory()))
                .findFirst()
                .orElse(null);

        if (sourceProduct == null || targetProduct == null) {
            log.debug("Rule '{}' skipped — one or both product categories not present in build",
                    rule.getIncompatibilityReason());
            return true;
        }

        String sourceValue = sourceProduct.getAttributeValue(rule.getSourceAttributeName());
        String targetValue = targetProduct.getAttributeValue(rule.getTargetAttributeName());

        return switch (rule.getOperator().toUpperCase()) {
            case "EQ", "EQUALS" -> evaluateEquals(rule, sourceValue, targetValue);
            case "GTE"          -> evaluateGte(rule, sourceValue, targetValue);
            case "CONTAINS"     -> evaluateContains(rule, sourceValue, targetValue);
            default -> {
                log.warn("Unknown operator '{}' in rule id={} — skipping (pass)",
                        rule.getOperator(), rule.getId());
                yield true;
            }
        };
    }

    /**
     * EQ / EQUALS: source attribute value must equal target attribute value (case-insensitive).
     * Returns false (incompatible) when either value is missing.
     */
    private boolean evaluateEquals(CompatibilityRule rule, String sourceValue, String targetValue) {
        if (sourceValue == null || targetValue == null) {
            log.warn("Rule '{}': EQ evaluation skipped — null attribute (source={}, target={})",
                    rule.getIncompatibilityReason(), sourceValue, targetValue);
            return false;
        }
        boolean result = sourceValue.equalsIgnoreCase(targetValue);
        log.debug("EQ '{}' vs '{}' → {}", sourceValue, targetValue, result);
        return result;
    }

    /**
     * GTE: source numeric value must be less than or equal to target numeric value.
     * E.g. GPU power draw (source) <= PSU wattage (target).
     * Returns false (incompatible) when either value is missing or non-numeric.
     */
    private boolean evaluateGte(CompatibilityRule rule, String sourceValue, String targetValue) {
        if (sourceValue == null || targetValue == null) {
            log.warn("Rule '{}': GTE evaluation skipped — null attribute (source={}, target={})",
                    rule.getIncompatibilityReason(), sourceValue, targetValue);
            return false;
        }
        try {
            double source = Double.parseDouble(sourceValue.trim());
            double target = Double.parseDouble(targetValue.trim());
            boolean result = source <= target;
            log.debug("GTE {} <= {} → {}", source, target, result);
            return result;
        } catch (NumberFormatException e) {
            log.warn("Rule '{}': GTE evaluation failed — non-numeric values (source='{}', target='{}')",
                    rule.getIncompatibilityReason(), sourceValue, targetValue);
            return false;
        }
    }

    /**
     * CONTAINS: target attribute value must contain the source attribute value (case-insensitive).
     * E.g. Case Form Factor Support (target) contains Motherboard Form Factor (source).
     * Returns false (incompatible) when either value is missing.
     */
    private boolean evaluateContains(CompatibilityRule rule, String sourceValue, String targetValue) {
        if (sourceValue == null || targetValue == null) {
            log.warn("Rule '{}': CONTAINS evaluation skipped — null attribute (source={}, target={})",
                    rule.getIncompatibilityReason(), sourceValue, targetValue);
            return false;
        }
        boolean result = targetValue.toLowerCase().contains(sourceValue.toLowerCase());
        log.debug("CONTAINS '{}' in '{}' → {}", sourceValue, targetValue, result);
        return result;
    }

    // -------------------------------------------------------------------------
    // CRUD — rules
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // CRUD — checks
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Mappers
    // -------------------------------------------------------------------------

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
