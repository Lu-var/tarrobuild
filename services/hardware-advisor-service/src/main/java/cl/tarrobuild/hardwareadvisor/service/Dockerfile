package cl.tarrobuild.auth.controller;

import cl.tarrobuild.auth.dto.AuthResponse;
import cl.tarrobuild.auth.dto.LoginRequest;
import cl.tarrobuild.auth.dto.RegisterRequest;
import cl.tarrobuild.auth.service.AuthService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Operaciones para registro, inicio de sesión, cierre de sesión y validación de tokens de usuarios")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/validate")
    @Operation(summary = "Validar token JWT", description = "Extrae y valida el token enviado en el header Authorization, retornando la información del usuario autenticado")
    public ResponseEntity<AuthResponse> validate(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Claims info = authService.validateToken(token);
        String email = info.getSubject();
        Long userId = info.get("userId", Long.class);
        String role = info.get("role", String.class);
        return ResponseEntity.ok(new AuthResponse(userId, token, email, role));
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar un nuevo usuario", description = "Crea una cuenta en el sistema validando los datos obligatorios enviados en el cuerpo de la solicitud")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica las credenciales de un usuario (email y contraseña) y retorna su token JWT correspondiente")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Invalida el token JWT del usuario enviado en el header Authorization para revocar el acceso")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return ResponseEntity.ok().build();
    }
}