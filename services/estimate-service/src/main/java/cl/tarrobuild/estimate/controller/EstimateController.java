package cl.tarrobuild.estimate.controller;

import cl.tarrobuild.estimate.dto.EstimateRequest;
import cl.tarrobuild.estimate.dto.EstimateResponse;
import cl.tarrobuild.estimate.service.EstimateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estimate")
public class EstimateController {

    private final EstimateService estimateService;

    public EstimateController(EstimateService estimateService) {
        this.estimateService = estimateService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<EstimateResponse> calculateEstimate(
            @Valid @RequestBody EstimateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estimateService.calculate(request));
    }

    @GetMapping("/{buildId}")
    public ResponseEntity<EstimateResponse> getLatestEstimateByBuildId(
            @PathVariable Long buildId) {
        return ResponseEntity.ok(estimateService.getLatestEstimateByBuildId(buildId));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<EstimateResponse> getEstimateById(@PathVariable Long id) {
        return ResponseEntity.ok(estimateService.getEstimateById(id));
    }

    @GetMapping("/all/{buildId}")
    public ResponseEntity<List<EstimateResponse>> getEstimatesByBuildId(
            @PathVariable Long buildId) {
        return ResponseEntity.ok(estimateService.getEstimatesByBuildId(buildId));
    }
}
