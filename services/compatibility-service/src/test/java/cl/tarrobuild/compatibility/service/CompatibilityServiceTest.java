package cl.tarrobuild.compatibility.service;

import cl.tarrobuild.compatibility.client.CategoryRestClient;
import cl.tarrobuild.compatibility.client.ProductRestClient;
import cl.tarrobuild.compatibility.dto.CategoryClientResponse;
import cl.tarrobuild.compatibility.dto.CompatibilityCheckRequest;
import cl.tarrobuild.compatibility.dto.CompatibilityCheckResponse;
import cl.tarrobuild.compatibility.dto.ProductAttributeClientResponse;
import cl.tarrobuild.compatibility.dto.ProductClientResponse;
import cl.tarrobuild.compatibility.model.CompatibilityRule;
import cl.tarrobuild.compatibility.repository.CompatibilityCheckRepository;
import cl.tarrobuild.compatibility.repository.CompatibilityRuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CompatibilityServiceTest {

    @Mock
    private CompatibilityRuleRepository ruleRepository;

    @Mock
    private CompatibilityCheckRepository checkRepository;

    @Mock
    private ProductRestClient productRestClient;

    @Mock
    private CategoryRestClient categoryRestClient;

    @InjectMocks
    private CompatibilityService compatibilityService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ProductClientResponse productResponse(Long id, Long categoryId, String... attrs) {
        List<ProductAttributeClientResponse> attributes = new ArrayList<>();
        for (int i = 0; i < attrs.length - 1; i += 2) {
            attributes.add(new ProductAttributeClientResponse(null, attrs[i], attrs[i + 1], null));
        }
        return new ProductClientResponse(id, "Product " + id, null, 0,
                categoryId, null, null, true, attributes);
    }

    private CompatibilityRule rule(String srcCat, String srcAttr, String op,
                                   String tgtCat, String tgtAttr, String reason) {
        CompatibilityRule r = new CompatibilityRule();
        r.setSourceCategory(srcCat);
        r.setSourceAttributeName(srcAttr);
        r.setOperator(op);
        r.setTargetCategory(tgtCat);
        r.setTargetAttributeName(tgtAttr);
        r.setIncompatibilityReason(reason);
        return r;
    }

    // -------------------------------------------------------------------------
    // Test 1 — no rules → compatible by default
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería responder compatible por defecto si no existen reglas cargadas")
    void check_NoRules_ReturnsCompatibleDefault() {
        CompatibilityCheckRequest request = new CompatibilityCheckRequest(100L, List.of(1L, 2L));

        Mockito.when(ruleRepository.findAll()).thenReturn(new ArrayList<>());
        Mockito.when(checkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompatibilityCheckResponse response = compatibilityService.check(request);

        assertNotNull(response);
        assertTrue(response.result());
        assertEquals("No rules defined — no incompatibilities found.", response.details());
    }

    // -------------------------------------------------------------------------
    // Test 2 — EQ: incompatible sockets
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería detectar incompatibilidad si los sockets de CPU y Motherboard no coinciden")
    void check_IncompatibleSockets_ReturnsFalseWithDetails() {
        CompatibilityCheckRequest request = new CompatibilityCheckRequest(101L, List.of(1L, 2L));

        CompatibilityRule socketRule = rule(
                "CPU", "Socket", "EQUALS",
                "Motherboard", "Socket",
                "El socket del procesador no es compatible con la placa madre.");

        Mockito.when(ruleRepository.findAll()).thenReturn(List.of(socketRule));

        ProductClientResponse cpu = productResponse(1L, 10L, "Socket", "AM4");
        ProductClientResponse motherboard = productResponse(2L, 20L, "Socket", "LGA1700");

        Mockito.when(productRestClient.getProductById(1L)).thenReturn(cpu);
        Mockito.when(productRestClient.getProductById(2L)).thenReturn(motherboard);
        Mockito.when(categoryRestClient.getCategoryById(10L)).thenReturn(new CategoryClientResponse(10L, "CPU"));
        Mockito.when(categoryRestClient.getCategoryById(20L)).thenReturn(new CategoryClientResponse(20L, "Motherboard"));
        Mockito.when(checkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompatibilityCheckResponse response = compatibilityService.check(request);

        assertNotNull(response);
        assertFalse(response.result());
        assertTrue(response.details().contains("El socket del procesador no es compatible con la placa madre."));
    }

    // -------------------------------------------------------------------------
    // Test 3 — GTE: GPU power draw exceeds PSU wattage → incompatible
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería detectar incompatibilidad si el power draw de la GPU supera el wattage del PSU")
    void check_GTE_InsufficientPSU_ReturnsFalse() {
        CompatibilityCheckRequest request = new CompatibilityCheckRequest(102L, List.of(1L, 2L));

        CompatibilityRule powerRule = rule(
                "GPU", "Power Draw", "GTE",
                "PSU", "Wattage",
                "Power supply wattage is insufficient for GPU power draw");

        Mockito.when(ruleRepository.findAll()).thenReturn(List.of(powerRule));

        ProductClientResponse gpu = productResponse(1L, 30L, "Power Draw", "350");
        ProductClientResponse psu = productResponse(2L, 40L, "Wattage", "300");

        Mockito.when(productRestClient.getProductById(1L)).thenReturn(gpu);
        Mockito.when(productRestClient.getProductById(2L)).thenReturn(psu);
        Mockito.when(categoryRestClient.getCategoryById(30L)).thenReturn(new CategoryClientResponse(30L, "GPU"));
        Mockito.when(categoryRestClient.getCategoryById(40L)).thenReturn(new CategoryClientResponse(40L, "PSU"));
        Mockito.when(checkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompatibilityCheckResponse response = compatibilityService.check(request);

        assertNotNull(response);
        assertFalse(response.result());
        assertTrue(response.details().contains("Power supply wattage is insufficient for GPU power draw"));
    }

    // -------------------------------------------------------------------------
    // Test 4 — GTE: GPU power draw within PSU wattage → compatible
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería confirmar compatibilidad si el wattage del PSU es suficiente para la GPU")
    void check_GTE_SufficientPSU_ReturnsTrue() {
        CompatibilityCheckRequest request = new CompatibilityCheckRequest(103L, List.of(1L, 2L));

        CompatibilityRule powerRule = rule(
                "GPU", "Power Draw", "GTE",
                "PSU", "Wattage",
                "Power supply wattage is insufficient for GPU power draw");

        Mockito.when(ruleRepository.findAll()).thenReturn(List.of(powerRule));

        ProductClientResponse gpu = productResponse(1L, 30L, "Power Draw", "250");
        ProductClientResponse psu = productResponse(2L, 40L, "Wattage", "650");

        Mockito.when(productRestClient.getProductById(1L)).thenReturn(gpu);
        Mockito.when(productRestClient.getProductById(2L)).thenReturn(psu);
        Mockito.when(categoryRestClient.getCategoryById(30L)).thenReturn(new CategoryClientResponse(30L, "GPU"));
        Mockito.when(categoryRestClient.getCategoryById(40L)).thenReturn(new CategoryClientResponse(40L, "PSU"));
        Mockito.when(checkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompatibilityCheckResponse response = compatibilityService.check(request);

        assertNotNull(response);
        assertTrue(response.result());
    }

    // -------------------------------------------------------------------------
    // Test 5 — CONTAINS: case form factor does not support motherboard form factor
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería detectar incompatibilidad si el case no soporta el form factor de la placa madre")
    void check_CONTAINS_UnsupportedFormFactor_ReturnsFalse() {
        CompatibilityCheckRequest request = new CompatibilityCheckRequest(104L, List.of(1L, 2L));

        CompatibilityRule formRule = rule(
                "Motherboard", "Form Factor", "CONTAINS",
                "Case", "Form Factor Support",
                "Case does not support motherboard form factor");

        Mockito.when(ruleRepository.findAll()).thenReturn(List.of(formRule));

        ProductClientResponse motherboard = productResponse(1L, 20L, "Form Factor", "E-ATX");
        ProductClientResponse pcCase      = productResponse(2L, 50L, "Form Factor Support", "Mini-ITX, Micro-ATX");

        Mockito.when(productRestClient.getProductById(1L)).thenReturn(motherboard);
        Mockito.when(productRestClient.getProductById(2L)).thenReturn(pcCase);
        Mockito.when(categoryRestClient.getCategoryById(20L)).thenReturn(new CategoryClientResponse(20L, "Motherboard"));
        Mockito.when(categoryRestClient.getCategoryById(50L)).thenReturn(new CategoryClientResponse(50L, "Case"));
        Mockito.when(checkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompatibilityCheckResponse response = compatibilityService.check(request);

        assertNotNull(response);
        assertFalse(response.result());
        assertTrue(response.details().contains("Case does not support motherboard form factor"));
    }

    // -------------------------------------------------------------------------
    // Test 6 — CONTAINS: case form factor supports motherboard form factor
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería confirmar compatibilidad si el case soporta el form factor de la placa madre")
    void check_CONTAINS_SupportedFormFactor_ReturnsTrue() {
        CompatibilityCheckRequest request = new CompatibilityCheckRequest(105L, List.of(1L, 2L));

        CompatibilityRule formRule = rule(
                "Motherboard", "Form Factor", "CONTAINS",
                "Case", "Form Factor Support",
                "Case does not support motherboard form factor");

        Mockito.when(ruleRepository.findAll()).thenReturn(List.of(formRule));

        ProductClientResponse motherboard = productResponse(1L, 20L, "Form Factor", "ATX");
        ProductClientResponse pcCase      = productResponse(2L, 50L, "Form Factor Support", "Mini-ITX, Micro-ATX, ATX");

        Mockito.when(productRestClient.getProductById(1L)).thenReturn(motherboard);
        Mockito.when(productRestClient.getProductById(2L)).thenReturn(pcCase);
        Mockito.when(categoryRestClient.getCategoryById(20L)).thenReturn(new CategoryClientResponse(20L, "Motherboard"));
        Mockito.when(categoryRestClient.getCategoryById(50L)).thenReturn(new CategoryClientResponse(50L, "Case"));
        Mockito.when(checkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompatibilityCheckResponse response = compatibilityService.check(request);

        assertNotNull(response);
        assertTrue(response.result());
    }

    // -------------------------------------------------------------------------
    // Test 7 — null attribute → incompatible
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería marcar incompatible si el producto no tiene el atributo requerido por la regla")
    void check_NullAttribute_ReturnsFalse() {
        CompatibilityCheckRequest request = new CompatibilityCheckRequest(106L, List.of(1L, 2L));

        CompatibilityRule socketRule = rule(
                "CPU", "Socket", "EQUALS",
                "Motherboard", "Socket",
                "El socket del procesador no es compatible con la placa madre.");

        Mockito.when(ruleRepository.findAll()).thenReturn(List.of(socketRule));

        ProductClientResponse cpu         = productResponse(1L, 10L /* no attrs */);
        ProductClientResponse motherboard = productResponse(2L, 20L, "Socket", "AM4");

        Mockito.when(productRestClient.getProductById(1L)).thenReturn(cpu);
        Mockito.when(productRestClient.getProductById(2L)).thenReturn(motherboard);
        Mockito.when(categoryRestClient.getCategoryById(10L)).thenReturn(new CategoryClientResponse(10L, "CPU"));
        Mockito.when(categoryRestClient.getCategoryById(20L)).thenReturn(new CategoryClientResponse(20L, "Motherboard"));
        Mockito.when(checkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompatibilityCheckResponse response = compatibilityService.check(request);

        assertNotNull(response);
        assertFalse(response.result());
    }

    // -------------------------------------------------------------------------
    // Test 8 — multiple rules, at least one fails → overall incompatible
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería marcar incompatible si al menos una regla falla entre varias")
    void check_MultipleRules_OneFails_ReturnsFalse() {
        CompatibilityCheckRequest request = new CompatibilityCheckRequest(107L, List.of(1L, 2L, 3L, 4L));

        CompatibilityRule socketRule = rule(
                "CPU", "Socket", "EQUALS",
                "Motherboard", "Socket",
                "El socket del procesador no es compatible con la placa madre.");
        CompatibilityRule powerRule = rule(
                "GPU", "Power Draw", "GTE",
                "PSU", "Wattage",
                "Power supply wattage is insufficient for GPU power draw");

        Mockito.when(ruleRepository.findAll()).thenReturn(List.of(socketRule, powerRule));

        ProductClientResponse cpu         = productResponse(1L, 10L, "Socket", "AM4");
        ProductClientResponse motherboard = productResponse(2L, 20L, "Socket", "AM4");
        ProductClientResponse gpu         = productResponse(3L, 30L, "Power Draw", "350");
        ProductClientResponse psu         = productResponse(4L, 40L, "Wattage", "300");

        Mockito.when(productRestClient.getProductById(1L)).thenReturn(cpu);
        Mockito.when(productRestClient.getProductById(2L)).thenReturn(motherboard);
        Mockito.when(productRestClient.getProductById(3L)).thenReturn(gpu);
        Mockito.when(productRestClient.getProductById(4L)).thenReturn(psu);
        Mockito.when(categoryRestClient.getCategoryById(10L)).thenReturn(new CategoryClientResponse(10L, "CPU"));
        Mockito.when(categoryRestClient.getCategoryById(20L)).thenReturn(new CategoryClientResponse(20L, "Motherboard"));
        Mockito.when(categoryRestClient.getCategoryById(30L)).thenReturn(new CategoryClientResponse(30L, "GPU"));
        Mockito.when(categoryRestClient.getCategoryById(40L)).thenReturn(new CategoryClientResponse(40L, "PSU"));
        Mockito.when(checkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompatibilityCheckResponse response = compatibilityService.check(request);

        assertNotNull(response);
        assertFalse(response.result());
        assertTrue(response.details().contains("Power supply wattage is insufficient for GPU power draw"));
    }

    // -------------------------------------------------------------------------
    // Test 9 — multiple rules, all pass → compatible
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería confirmar compatibilidad si todas las reglas se cumplen")
    void check_MultipleRules_AllPass_ReturnsTrue() {
        CompatibilityCheckRequest request = new CompatibilityCheckRequest(108L, List.of(1L, 2L, 3L));

        CompatibilityRule socketRule = rule(
                "CPU", "Socket", "EQUALS",
                "Motherboard", "Socket",
                "El socket del procesador no es compatible con la placa madre.");
        CompatibilityRule powerRule = rule(
                "GPU", "Power Draw", "GTE",
                "PSU", "Wattage",
                "Power supply wattage is insufficient for GPU power draw");

        Mockito.when(ruleRepository.findAll()).thenReturn(List.of(socketRule, powerRule));

        ProductClientResponse cpu         = productResponse(1L, 10L, "Socket", "AM4");
        ProductClientResponse motherboard = productResponse(2L, 20L, "Socket", "AM4");
        ProductClientResponse psu         = productResponse(3L, 40L, "Wattage", "650");

        Mockito.when(productRestClient.getProductById(1L)).thenReturn(cpu);
        Mockito.when(productRestClient.getProductById(2L)).thenReturn(motherboard);
        Mockito.when(productRestClient.getProductById(3L)).thenReturn(psu);
        Mockito.when(categoryRestClient.getCategoryById(10L)).thenReturn(new CategoryClientResponse(10L, "CPU"));
        Mockito.when(categoryRestClient.getCategoryById(20L)).thenReturn(new CategoryClientResponse(20L, "Motherboard"));
        Mockito.when(categoryRestClient.getCategoryById(40L)).thenReturn(new CategoryClientResponse(40L, "PSU"));
        Mockito.when(checkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompatibilityCheckResponse response = compatibilityService.check(request);

        assertNotNull(response);
        assertTrue(response.result());
        assertTrue(response.details().contains("All components are compatible."));
    }

    // -------------------------------------------------------------------------
    // Test 10 — empty product list → compatible by default
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería responder compatible si la lista de productos está vacía")
    void check_EmptyProductList_ReturnsCompatible() {
        CompatibilityCheckRequest request = new CompatibilityCheckRequest(109L, List.of());

        Mockito.when(ruleRepository.findAll()).thenReturn(List.of(
                rule("CPU", "Socket", "EQUALS", "Motherboard", "Socket", "Socket mismatch")));
        Mockito.when(checkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompatibilityCheckResponse response = compatibilityService.check(request);

        assertNotNull(response);
        assertTrue(response.result());
        assertTrue(response.details().contains("All components are compatible."));
    }

    // -------------------------------------------------------------------------
    // Test 11 — product returns null from Feign → handled gracefully
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería tolerar productos nulos del Feign client sin lanzar excepción")
    void check_NullProductFromFeign_SkipsGracefully() {
        CompatibilityCheckRequest request = new CompatibilityCheckRequest(110L, List.of(1L, 2L));

        Mockito.when(ruleRepository.findAll()).thenReturn(List.of(
                rule("CPU", "Socket", "EQUALS", "Motherboard", "Socket", "Socket mismatch")));

        Mockito.when(productRestClient.getProductById(1L)).thenReturn(null);
        Mockito.when(productRestClient.getProductById(2L)).thenReturn(null);
        Mockito.when(checkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompatibilityCheckResponse response = compatibilityService.check(request);

        assertNotNull(response);
        assertTrue(response.result());
        assertTrue(response.details().contains("All components are compatible."));
    }

    // -------------------------------------------------------------------------
    // Test 12 — very large attribute values (edge case)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería manejar valores numéricos extremadamente grandes en reglas GTE")
    void check_VeryLargeNumericValues_HandledGracefully() {
        CompatibilityCheckRequest request = new CompatibilityCheckRequest(111L, List.of(1L, 2L));

        CompatibilityRule powerRule = rule(
                "GPU", "Power Draw", "GTE",
                "PSU", "Wattage",
                "Power supply wattage is insufficient for GPU power draw");

        Mockito.when(ruleRepository.findAll()).thenReturn(List.of(powerRule));

        ProductClientResponse gpu = productResponse(1L, 30L, "Power Draw", "999999999999");
        ProductClientResponse psu = productResponse(2L, 40L, "Wattage", "999999999999");

        Mockito.when(productRestClient.getProductById(1L)).thenReturn(gpu);
        Mockito.when(productRestClient.getProductById(2L)).thenReturn(psu);
        Mockito.when(categoryRestClient.getCategoryById(30L)).thenReturn(new CategoryClientResponse(30L, "GPU"));
        Mockito.when(categoryRestClient.getCategoryById(40L)).thenReturn(new CategoryClientResponse(40L, "PSU"));
        Mockito.when(checkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompatibilityCheckResponse response = compatibilityService.check(request);

        assertNotNull(response);
        assertTrue(response.result());
    }
}
