package cl.tarrobuild.auth.client;

import cl.tarrobuild.auth.dto.UserClientRequest;
import cl.tarrobuild.auth.dto.UserClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class UserRestClient {

    private final RestClient restClient;

    public UserRestClient(RestClient.Builder builder,
                          @Value("${user-service.url}") String userServiceUrl) {
        this.restClient = builder
                .baseUrl(userServiceUrl)
                .build();
    }

    public UserClientResponse createUser(UserClientRequest request) {
        log.info("Calling user-service: POST /api/users");
        return restClient.post()
                .uri("/api/users")
                .body(request)
                .retrieve()
                .body(UserClientResponse.class);
    }

    public UserClientResponse getUserById(Long id) {
        log.info("Calling user-service: GET /api/users/{}", id);
        return restClient.get()
                .uri("api/users/{id}", id)
                .retrieve()
                .body(UserClientResponse.class);
    }
}
