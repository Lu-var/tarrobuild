package cl.tarrobuild.hardwareadvisor.controller;

import cl.tarrobuild.hardwareadvisor.dto.GenerateRecommendationsRequest;
import cl.tarrobuild.hardwareadvisor.dto.RecommendationResponse;
import cl.tarrobuild.hardwareadvisor.service.HardwareAdvisorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@Tag(name = "Recommendations", description = "Recomendaciones y sugerencias de componentes")
public class HardwareAdvisorController {

    private final HardwareAdvisorService hardwareAdvisorService;

    public HardwareAdvisorController(HardwareAdvisorService hardwareAdvisorService) {
        this.hardwareAdvisorService = hardwareAdvisorService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generar recomendaciones de componentes", description = "Genera recomendaciones de hardware basadas en las necesidades y presupuesto del usuario")
    public ResponseEntity<List<RecommendationResponse>> generateRecommendations(
            @Valid @RequestBody GenerateRecommendationsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hardwareAdvisorService.generate(request));
    }

    @GetMapping("/{buildId}")
    @Operation(summary = "Obtener recomendaciones por build", description = "Retorna todas las recomendaciones asociadas a un build específico")
    public ResponseEntity<List<RecommendationResponse>> getRecommendationsByBuildId(
            @PathVariable Long buildId) {
        return ResponseEntity.ok(hardwareAdvisorService.getRecommendationsByBuildId(buildId));
    }

    @GetMapping("/id/{id}")
    @Operation(summary = "Obtener recomendación por ID", description = "Retorna una recomendación específica según su identificador único")
    public ResponseEntity<RecommendationResponse> getRecommendationById(@PathVariable Long id) {
        return ResponseEntity.ok(hardwareAdvisorService.getRecommendationById(id));
    }
}
