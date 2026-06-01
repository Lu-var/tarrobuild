package cl.tarrobuild.compatibility.controller;

import cl.tarrobuild.compatibility.dto.CompatibilityCheckRequest;
import cl.tarrobuild.compatibility.dto.CompatibilityCheckResponse;
import cl.tarrobuild.compatibility.dto.CompatibilityRuleRequest;
import cl.tarrobuild.compatibility.dto.CompatibilityRuleResponse;
import cl.tarrobuild.compatibility.service.CompatibilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compatibility")
public class CompatibilityController {

    private final CompatibilityService compatibilityService;

    public CompatibilityController(CompatibilityService compatibilityService) {
        this.compatibilityService = compatibilityService;
    }

    @PostMapping("/check")
    public ResponseEntity<CompatibilityCheckResponse> checkCompatibility(
            @Valid @RequestBody CompatibilityCheckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(compatibilityService.check(request));
    }

    @GetMapping("/check/{buildId}")
    public ResponseEntity<CompatibilityCheckResponse> getLatestCheckByBuildId(
            @PathVariable Long buildId) {
        return ResponseEntity.ok(compatibilityService.getLatestCheckByBuildId(buildId));
    }

    @GetMapping("/check/id/{id}")
    public ResponseEntity<CompatibilityCheckResponse> getCheckById(@PathVariable Long id) {
        return ResponseEntity.ok(compatibilityService.getCheckById(id));
    }

    @PostMapping("/rules")
    public ResponseEntity<CompatibilityRuleResponse> createRule(
            @Valid @RequestBody CompatibilityRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(compatibilityService.createRule(request));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<CompatibilityRuleResponse>> getAllRules() {
        return ResponseEntity.ok(compatibilityService.getAllRules());
    }

    @GetMapping("/rules/{id}")
    public ResponseEntity<CompatibilityRuleResponse> getRuleById(@PathVariable Long id) {
        return ResponseEntity.ok(compatibilityService.getRuleById(id));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<CompatibilityRuleResponse> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody CompatibilityRuleRequest request) {
        return ResponseEntity.ok(compatibilityService.updateRule(id, request));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        boolean deleted = compatibilityService.deleteRule(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
