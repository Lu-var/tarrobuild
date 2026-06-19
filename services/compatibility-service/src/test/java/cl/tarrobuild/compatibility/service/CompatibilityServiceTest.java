package cl.tarrobuild.compatibility.service;

import cl.tarrobuild.compatibility.dto.CompatibilityCheckRequest;
import cl.tarrobuild.compatibility.dto.CompatibilityCheckResponse;
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
}