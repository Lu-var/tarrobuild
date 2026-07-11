package cl.tarrobuild.build.controller;

import cl.tarrobuild.build.dto.*;
import cl.tarrobuild.build.service.BuildService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/builds")
@Tag(name = "Builds", description = "Gestión de configuraciones de PC")
public class BuildController {

    private final BuildService buildService;

    public BuildController(BuildService buildService) {
        this.buildService = buildService;
    }

    @GetMapping
    @Operation(summary = "Obtener builds (ADMIN: todas, USER: solo las propias)")
    public ResponseEntity<List<BuildResponse>> getBuilds(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        if ("ADMIN".equals(role)) {
            return ResponseEntity.ok(buildService.getAllBuilds());
        }
        return ResponseEntity.ok(buildService.getBuildsByUserId(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una build por su ID")
    @ApiResponse(responseCode = "404", description = "Build no encontrado")
    public ResponseEntity<BuildResponse> getBuildById(@PathVariable Long id) {
        return ResponseEntity.ok(buildService.getBuildById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Obtener todas las builds de un usuario específico")
    @ApiResponse(responseCode = "403", description = "No autorizado para ver builds de otro usuario")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    public ResponseEntity<List<BuildResponse>> getBuildsByUserId(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role) && !requesterId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(buildService.getBuildsByUserId(userId));
    }

    @GetMapping("/user/{userId}/{id}")
    @Operation(summary = "Obtener una build específica de un usuario")
    @ApiResponse(responseCode = "403", description = "No autorizado para ver builds de otro usuario")
    @ApiResponse(responseCode = "404", description = "Build o usuario no encontrado")
    public ResponseEntity<BuildResponse> getBuildByIdAndUserId(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role) && !requesterId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(buildService.getBuildByIdAndUserId(id, userId));
    }

    @PostMapping
    @Operation(summary = "Crear una nueva configuración de PC")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    public ResponseEntity<BuildResponse> createBuild(@Valid @RequestBody BuildRequest request) {
        BuildResponse created = buildService.createBuild(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una build existente")
    @ApiResponse(responseCode = "404", description = "Build no encontrado")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    public ResponseEntity<BuildResponse> updateBuild(@PathVariable Long id, @Valid @RequestBody BuildRequest request) {
        return ResponseEntity.ok(buildService.updateBuild(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Actualizar el estado de una build")
    @ApiResponse(responseCode = "404", description = "Build no encontrado")
    @ApiResponse(responseCode = "400", description = "Estado inválido")
    public ResponseEntity<BuildResponse> updateBuildStatus(@PathVariable Long id, @Valid @RequestBody BuildStatusRequest request) {
        return ResponseEntity.ok(buildService.updateBuildStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una build")
    @ApiResponse(responseCode = "404", description = "Build no encontrado")
    public ResponseEntity<Void> deleteBuild(@PathVariable Long id) {
        if (!buildService.deleteBuild(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{buildId}/items")
    @Operation(summary = "Obtener todos los items de una build")
    @ApiResponse(responseCode = "404", description = "Build no encontrado")
    public ResponseEntity<List<BuildItemResponse>> getItems(@PathVariable Long buildId) {
        return ResponseEntity.ok(buildService.getItemsByBuildId(buildId));
    }

    @PostMapping("/{buildId}/items")
    @Operation(summary = "Agregar un item a una build")
    @ApiResponse(responseCode = "404", description = "Build o producto no encontrado")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    public ResponseEntity<BuildItemResponse> createItem(
            @PathVariable Long buildId,
            @Valid @RequestBody BuildItemRequest request) {
        BuildItemResponse created = buildService.createItem(buildId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{buildId}/items/{itemId}")
    @Operation(summary = "Obtener un item específico de una build")
    @ApiResponse(responseCode = "404", description = "Item o build no encontrado")
    public ResponseEntity<BuildItemResponse> getItemById(@PathVariable Long buildId, @PathVariable Long itemId) {
        return ResponseEntity.ok(buildService.getItemByIdAndBuildId(itemId, buildId));
    }

    @PutMapping("/{buildId}/items/{itemId}")
    @Operation(summary = "Actualizar un item de una build")
    @ApiResponse(responseCode = "404", description = "Item o build no encontrado")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    public ResponseEntity<BuildItemResponse> updateItem(@PathVariable Long buildId, @PathVariable Long itemId, @Valid @RequestBody BuildItemRequest request) {
        return ResponseEntity.ok(buildService.updateItem(buildId, itemId, request));
    }

    @DeleteMapping("/{buildId}/items/{itemId}")
    @Operation(summary = "Eliminar un item de una build")
    @ApiResponse(responseCode = "404", description = "Item o build no encontrado")
    public ResponseEntity<Void> deleteItem(@PathVariable Long buildId, @PathVariable Long itemId) {
        buildService.deleteItem(itemId, buildId);
        return ResponseEntity.noContent().build();
    }
}