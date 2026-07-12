package cl.tarrobuild.notification.client;

import cl.tarrobuild.notification.dto.UserClientResponse;
import lombok.extern.slf4j.Slf4j;
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

    public UserClientResponse getUserById(Long id) {
        log.info("Calling user-service: GET /api/users/{}", id);
        return restClient.get()
                .uri("/api/users/{id}", id)
                .retrieve()
                .body(UserClientResponse.class);
    }
}
