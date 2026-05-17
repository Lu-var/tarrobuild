package cl.tarrobuild.auth.service;

import cl.tarrobuild.auth.client.UserRestClient;
import cl.tarrobuild.auth.config.JwtUtil;
import cl.tarrobuild.auth.dto.AuthResponse;
import cl.tarrobuild.auth.dto.RegisterRequest;
import cl.tarrobuild.auth.dto.UserClientRequest;
import cl.tarrobuild.auth.dto.UserClientResponse;
import cl.tarrobuild.auth.model.Credential;
import cl.tarrobuild.auth.repository.CredentialRepository;
import jakarta.persistence.EntityExistsException;
import lombok.extern.slf4j.Slf4j;
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

        return new AuthResponse(token, saved.getEmail(), saved.getRole());
    }
}
