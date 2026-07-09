package cl.tarrobuild.estimate.controller;

import cl.tarrobuild.estimate.dto.EstimateRequest;
import cl.tarrobuild.estimate.dto.EstimateResponse;
import cl.tarrobuild.estimate.service.EstimateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estimate")
@Tag(name = "Estimates", description = "Cálculo de costos de builds")
public class EstimateController {

    private final EstimateService estimateService;

    public EstimateController(EstimateService estimateService) {
        this.estimateService = estimateService;
    }

    @PostMapping("/calculate")
    @Operation(summary = "Calcular estimación de build", description = "Calcula el costo total de un build basado en los componentes seleccionados")
    public ResponseEntity<EstimateResponse> calculateEstimate(
            @Valid @RequestBody EstimateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estimateService.calculate(request));
    }

    @GetMapping("/{buildId}")
    @Operation(summary = "Obtener última estimación por build", description = "Retorna la estimación más reciente asociada a un build específico")
    public ResponseEntity<EstimateResponse> getLatestEstimateByBuildId(
            @PathVariable Long buildId) {
        return ResponseEntity.ok(estimateService.getLatestEstimateByBuildId(buildId));
    }

    @GetMapping("/id/{id}")
    @Operation(summary = "Obtener estimación por ID", description = "Retorna una estimación específica según su identificador único")
    public ResponseEntity<EstimateResponse> getEstimateById(@PathVariable Long id) {
        return ResponseEntity.ok(estimateService.getEstimateById(id));
    }

    @GetMapping("/all/{buildId}")
    @Operation(summary = "Listar estimaciones por build", description = "Retorna el historial completo de estimaciones para un build dado")
    public ResponseEntity<List<EstimateResponse>> getEstimatesByBuildId(
            @PathVariable Long buildId) {
        return ResponseEntity.ok(estimateService.getEstimatesByBuildId(buildId));
    }
}
