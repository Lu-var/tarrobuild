package cl.tarrobuild.hardwareadvisor.controller;

import cl.tarrobuild.hardwareadvisor.dto.GenerateRecommendationsRequest;
import cl.tarrobuild.hardwareadvisor.dto.RecommendationResponse;
import cl.tarrobuild.hardwareadvisor.service.HardwareAdvisorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class HardwareAdvisorController {

    private final HardwareAdvisorService hardwareAdvisorService;

    public HardwareAdvisorController(HardwareAdvisorService hardwareAdvisorService) {
        this.hardwareAdvisorService = hardwareAdvisorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<List<RecommendationResponse>> generateRecommendations(
            @Valid @RequestBody GenerateRecommendationsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hardwareAdvisorService.generate(request));
    }

    @GetMapping("/{buildId}")
    public ResponseEntity<List<RecommendationResponse>> getRecommendationsByBuildId(
            @PathVariable Long buildId) {
        return ResponseEntity.ok(hardwareAdvisorService.getRecommendationsByBuildId(buildId));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<RecommendationResponse> getRecommendationById(@PathVariable Long id) {
        return ResponseEntity.ok(hardwareAdvisorService.getRecommendationById(id));
    }
}
