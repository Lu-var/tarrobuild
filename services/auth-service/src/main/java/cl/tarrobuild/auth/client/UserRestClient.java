package cl.tarrobuild.auth.client;

import cl.tarrobuild.auth.dto.UserClientRequest;
import cl.tarrobuild.auth.dto.UserClientResponse;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class UserRestClient {

    private final RestClient restClient;

    public UserRestClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("lb://user-service")
                .build();
    }

    public UserClientResponse createUser(UserClientRequest request) {
        log.info("Calling user-service: POST /api/users");
        return restClient.post()
                .uri("/api/users")
                .body(request)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.CONFLICT.value(), (req, res) -> {
                    throw new EntityExistsException("Email already exists");
                })
                .body(UserClientResponse.class);
    }

    public UserClientResponse getUserById(Long id) {
        log.info("Calling user-service: GET /api/users/{}", id);
        return restClient.get()
                .uri("api/users/{id}", id)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {
                    throw new EntityNotFoundException("User with ID " + id + " not found");
                })
                .body(UserClientResponse.class);
    }
}
