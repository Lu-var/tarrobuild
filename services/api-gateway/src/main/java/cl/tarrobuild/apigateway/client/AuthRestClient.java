package cl.tarrobuild.apigateway.client;

import cl.tarrobuild.apigateway.dto.AuthClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class AuthRestClient {

    private final RestClient restClient;

    public AuthRestClient(RestClient.Builder builder,
                          @Value("${AUTH_SERVICE_URL:http://localhost:8081}") String authServiceUrl) {
        this.restClient = builder
                .baseUrl(authServiceUrl)
                .build();
    }

    public AuthClientResponse validateToken(String token) {
        log.info("Calling auth-service: GET /api/auth/validate");
        return restClient.get()
                .uri("/api/auth/validate")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(AuthClientResponse.class);
    }
}
