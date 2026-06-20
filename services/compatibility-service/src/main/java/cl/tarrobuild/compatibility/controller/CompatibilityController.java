package cl.tarrobuild.compatibility.controller;

import cl.tarrobuild.compatibility.dto.CompatibilityCheckRequest;
import cl.tarrobuild.compatibility.dto.CompatibilityCheckResponse;
import cl.tarrobuild.compatibility.dto.CompatibilityRuleRequest;
import cl.tarrobuild.compatibility.dto.CompatibilityRuleResponse;
import cl.tarrobuild.compatibility.service.CompatibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Módulo de Compatibilidad", description = "Endpoints para gestionar reglas de hardware y evaluar la compatibilidad de armados (Builds)")
@RestController
@RequestMapping("/api/compatibility")
public class CompatibilityController {

    private final CompatibilityService compatibilityService;

    public CompatibilityController(CompatibilityService compatibilityService) {
        this.compatibilityService = compatibilityService;
    }

    @Operation(summary = "Evaluar compatibilidad", description = "Ejecuta las reglas de negocio distribuidas contrastando los atributos del hardware de un armado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Evaluación ejecutada e historial guardado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o mal estructurada")
    })
    @PostMapping("/check")
    public ResponseEntity<CompatibilityCheckResponse> checkCompatibility(
            @Valid @RequestBody CompatibilityCheckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(compatibilityService.check(request));
    }

    @Operation(summary = "Obtener última validación por Build ID", description = "Recupera el último informe de compatibilidad registrado para una cotización específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Informe de validación encontrado"),
            @ApiResponse(responseCode = "404", description = "No se encontraron registros para el Build ID entregado")
    })
    @GetMapping("/check/{buildId}")
    public ResponseEntity<CompatibilityCheckResponse> getLatestCheckByBuildId(
            @PathVariable Long buildId) {
        return ResponseEntity.ok(compatibilityService.getLatestCheckByBuildId(buildId));
    }

    @Operation(summary = "Buscar validación por ID", description = "Busca un registro de análisis de compatibilidad único mediante su ID primario.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro encontrado con éxito"),
            @ApiResponse(responseCode = "404", description = "ID de validación inexistente en el sistema")
    })
    @GetMapping("/check/id/{id}")
    public ResponseEntity<CompatibilityCheckResponse> getCheckById(@PathVariable Long id) {
        return ResponseEntity.ok(compatibilityService.getCheckById(id));
    }

    @Operation(summary = "Crear nueva regla de compatibilidad", description = "Registra una restricción técnica de hardware (Ej: Restringir que sockets 'AM4' operen con placas 'LGA1700').")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Regla de negocio creada y mapeada correctamente"),
            @ApiResponse(responseCode = "400", description = "Payload con datos obligatorios ausentes o corruptos")
    })
    @PostMapping("/rules")
    public ResponseEntity<CompatibilityRuleResponse> createRule(
            @Valid @RequestBody CompatibilityRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(compatibilityService.createRule(request));
    }

    @Operation(summary = "Listar todas las reglas", description = "Obtiene la colección de reglas de negocio cargadas en el microservicio distribuido.")
    @ApiResponse(responseCode = "200", description = "Lista recuperada exitosamente")
    @GetMapping("/rules")
    public ResponseEntity<List<CompatibilityRuleResponse>> getAllRules() {
        return ResponseEntity.ok(compatibilityService.getAllRules());
    }

    @Operation(summary = "Buscar regla por ID", description = "Recupera la configuración técnica detallada de una regla específica en base a su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Regla localizada correctamente"),
            @ApiResponse(responseCode = "404", description = "El ID ingresado no corresponde a ninguna regla de negocio")
    })
    @GetMapping("/rules/{id}")
    public ResponseEntity<CompatibilityRuleResponse> getRuleById(@PathVariable Long id) {
        return ResponseEntity.ok(compatibilityService.getRuleById(id));
    }

    @Operation(summary = "Actualizar una regla existente", description = "Modifica los parámetros funcionales, operadores o categorías de una regla en tiempo de ejecución.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Regla modificada con éxito"),
            @ApiResponse(responseCode = "404", description = "Regla no encontrada para la actualización")
    })
    @PutMapping("/rules/{id}")
    public ResponseEntity<CompatibilityRuleResponse> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody CompatibilityRuleRequest request) {
        return ResponseEntity.ok(compatibilityService.updateRule(id, request));
    }

    @Operation(summary = "Eliminar una regla", description = "Quita definitivamente una restricción del sistema del repositorio de compatibilidad.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "244", description = "No Content - Regla purgada del sistema satisfactoriamente"),
            @ApiResponse(responseCode = "404", description = "No se pudo eliminar porque el ID no existe en la base de datos")
    })
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        boolean deleted = compatibilityService.deleteRule(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}