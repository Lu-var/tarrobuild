package cl.tarrobuild.build.controller;

import cl.tarrobuild.build.dto.*;
import cl.tarrobuild.build.service.BuildService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/builds")
public class BuildController {

    private final BuildService buildService;

    public BuildController(BuildService buildService) {
        this.buildService = buildService;
    }

    @GetMapping
    public ResponseEntity<List<BuildResponse>> getBuilds(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(buildService.getBuildsByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuildResponse> getBuildById(@PathVariable Long id) {
        return ResponseEntity.ok(buildService.getBuildById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BuildResponse>> getBuildsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(buildService.getBuildsByUserId(userId));
    }

    @GetMapping("/user/{userId}/{id}")
    public ResponseEntity<BuildResponse> getBuildByIdAndUserId(@PathVariable Long id, @PathVariable Long userId) {
        return ResponseEntity.ok(buildService.getBuildByIdAndUserId(id, userId));
    }

    @PostMapping
    public ResponseEntity<BuildResponse> createBuild(@Valid @RequestBody BuildRequest request) {
        BuildResponse created = buildService.createBuild(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BuildResponse> updateBuild(@PathVariable Long id, @Valid @RequestBody BuildRequest request) {
        return ResponseEntity.ok(buildService.updateBuild(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BuildResponse> updateBuildStatus(@PathVariable Long id, @Valid @RequestBody BuildStatusRequest request) {
        return ResponseEntity.ok(buildService.updateBuildStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBuild(@PathVariable Long id) {
        if (!buildService.deleteBuild(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{buildId}/items")
    public ResponseEntity<List<BuildItemResponse>> getItems(@PathVariable Long buildId) {
        return ResponseEntity.ok(buildService.getItemsByBuildId(buildId));
    }

    @PostMapping("/{buildId}/items")
    public ResponseEntity<BuildItemResponse> createItem(
            @PathVariable Long buildId,
            @Valid @RequestBody BuildItemRequest request) {
        BuildItemResponse created = buildService.createItem(buildId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{buildId}/items/{itemId}")
    public ResponseEntity<BuildItemResponse> getItemById(@PathVariable Long buildId, @PathVariable Long itemId) {
        return ResponseEntity.ok(buildService.getItemByIdAndBuildId(itemId, buildId));
    }

    @PutMapping("/{buildId}/items/{itemId}")
    public ResponseEntity<BuildItemResponse> updateItem(@PathVariable Long buildId, @PathVariable Long itemId, @Valid @RequestBody BuildItemRequest request) {
        return ResponseEntity.ok(buildService.updateItem(buildId, itemId, request));
    }

    @DeleteMapping("/{buildId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long buildId, @PathVariable Long itemId) {
        buildService.deleteItem(itemId, buildId);
        return ResponseEntity.noContent().build();
    }
}