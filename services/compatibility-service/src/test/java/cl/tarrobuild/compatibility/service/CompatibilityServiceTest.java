package cl.tarrobuild.compatibility.service;

import cl.tarrobuild.compatibility.client.ProductClient;
import cl.tarrobuild.compatibility.dto.CompatibilityCheckRequest;
import cl.tarrobuild.compatibility.dto.CompatibilityCheckResponse;
import cl.tarrobuild.compatibility.dto.ProductDTO;
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
    private ProductClient productClient;

    @InjectMocks
    private CompatibilityService compatibilityService;

    @Test
    @DisplayName("Debería responder compatible por defecto si no existen reglas cargadas")
    void check_NoRules_ReturnsCompatibleDefault() {
        // GIVEN
        CompatibilityCheckRequest request = new CompatibilityCheckRequest(100L, List.of(1L, 2L));

        Mockito.when(ruleRepository.findAll()).thenReturn(new ArrayList<>());
        Mockito.when(checkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        CompatibilityCheckResponse response = compatibilityService.check(request);

        // THEN
        assertNotNull(response);
        assertTrue(response.result());
        assertEquals("No rules defined — no incompatibilities found.", response.details());
    }

    @Test
    @DisplayName("Debería detectar incompatibilidad si los sockets de CPU y Motherboard no coinciden")
    void check_IncompatibleSockets_ReturnsFalseWithDetails() {
        // GIVEN: Una solicitud con dos productos
        CompatibilityCheckRequest request = new CompatibilityCheckRequest(101L, List.of(1L, 2L));

        // Creamos una regla estricta de Socket
        CompatibilityRule rule = new CompatibilityRule();
        rule.setSourceCategory("CPU");
        rule.setSourceAttributeName("socketType");
        rule.setOperator("EQUALS");
        rule.setTargetCategory("Motherboard");
        rule.setTargetAttributeName("socketType");
        rule.setIncompatibilityReason("El socket del procesador no es compatible con la placa madre.");

        // Simulamos que la regla existe en la BD
        Mockito.when(ruleRepository.findAll()).thenReturn(List.of(rule));

        // Simulamos los productos remotos con sockets distintos usando records DTO
        ProductDTO cpu = new ProductDTO(1L, "Procesador AMD", "CPU", "AM4");
        ProductDTO motherboard = new ProductDTO(2L, "Placa Intel", "Motherboard", "LGA1700");

        Mockito.when(productClient.getProductById(1L)).thenReturn(cpu);
        Mockito.when(productClient.getProductById(2L)).thenReturn(motherboard);

        // Simular guardado en repositorio
        Mockito.when(checkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN: Evaluamos la compatibilidad
        CompatibilityCheckResponse response = compatibilityService.check(request);

        // THEN: Validamos que falle con los detalles correctos
        assertNotNull(response);
        assertFalse(response.result()); // Debe dar falso (Incompatible)
        assertTrue(response.details().contains("El socket del procesador no es compatible con la placa madre."));
    }
}