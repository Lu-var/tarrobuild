package cl.tarrobuild.auth.service;

import cl.tarrobuild.auth.client.UserRestClient;
import cl.tarrobuild.auth.config.JwtUtil;
import cl.tarrobuild.auth.dto.AuthResponse;
import cl.tarrobuild.auth.dto.LoginRequest;
import cl.tarrobuild.auth.dto.RegisterRequest;
import cl.tarrobuild.auth.dto.UserClientRequest;
import cl.tarrobuild.auth.dto.UserClientResponse;
import cl.tarrobuild.auth.model.Credential;
import cl.tarrobuild.auth.repository.CredentialRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SecurityException;
import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRestClient userRestClient;

    @InjectMocks
    private AuthService authService;

    private Credential credential;

    @BeforeEach
    void setUp() {
        credential = new Credential();
        credential.setId(1L);
        credential.setUserId(100L);
        credential.setEmail("john@example.com");
        credential.setPasswordHash("$2a$10$encodedHash");
        credential.setRole("USER");
    }

    // -------------------------------------------------------------------------
    // register() -- happy path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deberia registrar un nuevo usuario y devolver AuthResponse con token")
    void register_Successful_ReturnsAuthResponse() {
        RegisterRequest registerRequest = new RegisterRequest(
                "john@example.com",
                "password123",
                "John",
                "Doe",
                "+1234567890"
        );

        UserClientResponse userClientResponse = new UserClientResponse(
                100L,
                "John",
                "Doe",
                "john@example.com",
                "+1234567890",
                LocalDateTime.now()
        );

        Mockito.when(credentialRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        Mockito.when(passwordEncoder.encode(registerRequest.password())).thenReturn("$2a$10$encodedHash");
        Mockito.when(userRestClient.createUser(any(UserClientRequest.class))).thenReturn(userClientResponse);
        Mockito.when(credentialRepository.save(any(Credential.class))).thenReturn(credential);
        Mockito.when(jwtUtil.generateToken(100L, "john@example.com", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals(100L, response.userId());
        assertEquals("jwt-token", response.token());
        assertEquals("john@example.com", response.email());
        assertEquals("USER", response.role());
    }

    // -------------------------------------------------------------------------
    // register() -- duplicate email
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deberia lanzar EntityExistsException si el email ya esta registrado")
    void register_DuplicateEmail_ThrowsEntityExistsException() {
        RegisterRequest registerRequest = new RegisterRequest(
                "john@example.com",
                "password123",
                "John",
                "Doe",
                "+1234567890"
        );

        Mockito.when(credentialRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        assertThrows(EntityExistsException.class, () -> authService.register(registerRequest));
    }

    // -------------------------------------------------------------------------
    // register() -- user service failure
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deberia propagar la excepcion si el servicio de usuarios falla")
    void register_UserRestClientFails_PropagatesException() {
        RegisterRequest registerRequest = new RegisterRequest(
                "john@example.com",
                "password123",
                "John",
                "Doe",
                "+1234567890"
        );

        Mockito.when(credentialRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        Mockito.when(passwordEncoder.encode(registerRequest.password())).thenReturn("$2a$10$encodedHash");
        Mockito.when(userRestClient.createUser(any(UserClientRequest.class)))
                .thenThrow(new RuntimeException("User service unavailable"));

        assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
    }

    // -------------------------------------------------------------------------
    // login() -- happy path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deberia autenticar un usuario con credenciales validas y devolver AuthResponse")
    void login_ValidCredentials_ReturnsAuthResponse() {
        LoginRequest loginRequest = new LoginRequest(
                "john@example.com",
                "password123"
        );

        Mockito.when(credentialRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(credential));
        Mockito.when(passwordEncoder.matches(loginRequest.password(), credential.getPasswordHash())).thenReturn(true);
        Mockito.when(jwtUtil.generateToken(100L, "john@example.com", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals(100L, response.userId());
        assertEquals("jwt-token", response.token());
        assertEquals("john@example.com", response.email());
        assertEquals("USER", response.role());
    }

    // -------------------------------------------------------------------------
    // login() -- user not found
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deberia lanzar BadCredentialsException si el email no existe")
    void login_UserNotFound_ThrowsBadCredentialsException() {
        LoginRequest loginRequest = new LoginRequest(
                "john@example.com",
                "password123"
        );

        Mockito.when(credentialRepository.findByEmail(loginRequest.email())).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }

    // -------------------------------------------------------------------------
    // login() -- wrong password
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deberia lanzar BadCredentialsException si la contrasena es incorrecta")
    void login_WrongPassword_ThrowsBadCredentialsException() {
        LoginRequest loginRequest = new LoginRequest(
                "john@example.com",
                "password123"
        );

        Mockito.when(credentialRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(credential));
        Mockito.when(passwordEncoder.matches(loginRequest.password(), credential.getPasswordHash())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }

    // -------------------------------------------------------------------------
    // validateToken() -- valid token
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deberia devolver los Claims para un token valido")
    void validateToken_ValidToken_ReturnsClaims() {
        io.jsonwebtoken.Claims mockClaims = Mockito.mock(io.jsonwebtoken.Claims.class);
        Mockito.when(jwtUtil.validateToken("valid-token")).thenReturn(mockClaims);

        io.jsonwebtoken.Claims result = authService.validateToken("valid-token");

        assertNotNull(result);
        assertEquals(mockClaims, result);
    }

    // -------------------------------------------------------------------------
    // validateToken() -- expired token
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deberia lanzar BadCredentialsException si el token ha expirado")
    void validateToken_ExpiredToken_ThrowsBadCredentialsException() {
        Mockito.when(jwtUtil.validateToken("expired-token"))
                .thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        assertThrows(BadCredentialsException.class, () -> authService.validateToken("expired-token"));
    }

    // -------------------------------------------------------------------------
    // validateToken() -- invalid signature
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deberia lanzar BadCredentialsException si la firma del token es invalida")
    void validateToken_InvalidSignature_ThrowsBadCredentialsException() {
        Mockito.when(jwtUtil.validateToken("tampered-token"))
                .thenThrow(new SecurityException("JWT signature does not match"));

        assertThrows(BadCredentialsException.class, () -> authService.validateToken("tampered-token"));
    }

    // -------------------------------------------------------------------------
    // validateToken() -- malformed token
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deberia lanzar BadCredentialsException si el token esta malformado")
    void validateToken_MalformedToken_ThrowsBadCredentialsException() {
        Mockito.when(jwtUtil.validateToken("not-a-jwt"))
                .thenThrow(new MalformedJwtException("Malformed JWT"));

        assertThrows(BadCredentialsException.class, () -> authService.validateToken("not-a-jwt"));
    }

    // -------------------------------------------------------------------------
    // logout() -- smoke test
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deberia ejecutarse sin errores al solicitar logout")
    void logout_DoesNotThrow() {
        assertDoesNotThrow(() -> authService.logout("any-token"));
    }
}