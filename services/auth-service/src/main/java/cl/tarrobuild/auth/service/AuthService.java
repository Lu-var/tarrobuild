package cl.tarrobuild.auth.service;

import cl.tarrobuild.auth.client.UserRestClient;
import cl.tarrobuild.auth.config.JwtUtil;
import cl.tarrobuild.auth.dto.*;
import cl.tarrobuild.auth.model.Credential;
import cl.tarrobuild.auth.repository.CredentialRepository;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private final CredentialRepository credentialRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserRestClient userRestClient;

    public AuthService(CredentialRepository credentialRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       UserRestClient userRestClient) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userRestClient = userRestClient;
    }

    public AuthResponse register(RegisterRequest request) {
        if (credentialRepository.existsByEmail(request.email())) {
            throw new EntityExistsException("Email " + request.email() + " already exists");
        }

        Credential newCredentials = new Credential();
        String passwordHash = passwordEncoder.encode(request.password());
        UserClientRequest newUserRequest = new UserClientRequest(request.name(), request.lastName(), request.email(), request.phone());
        UserClientResponse userResponse = userRestClient.createUser(newUserRequest);

        newCredentials.setUserId(userResponse.id());
        newCredentials.setEmail(userResponse.email());
        newCredentials.setPasswordHash(passwordHash);
        newCredentials.setRole("USER");
        Credential saved = credentialRepository.save(newCredentials);
        String token = jwtUtil.generateToken(saved.getUserId(), saved.getEmail(), saved.getRole());

        return new AuthResponse(saved.getUserId(), token, saved.getEmail(), saved.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        Credential credential = credentialRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        boolean isValidLogin = passwordEncoder.matches(request.password(), credential.getPasswordHash());
        if (!isValidLogin) {
            throw new BadCredentialsException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(credential.getUserId(), credential.getEmail(), credential.getRole());
        return new AuthResponse(credential.getUserId(), token, credential.getEmail(), credential.getRole());
    }

    public Claims validateToken(String token) {
        try {
            return jwtUtil.validateToken(token);
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid or expired token");
        }
    }

    public void logout(String token) {
        log.info("Logout requested for token");
        // Placeholder for blacklist logic
    }
}
