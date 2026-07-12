package cl.tarrobuild.estimate.client;

import cl.tarrobuild.estimate.dto.BuildClientResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class BuildRestClient {

    private final RestClient restClient;

    public BuildRestClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("lb://build-service")
                .build();
    }

    public BuildClientResponse getBuildById(Long id) {
        log.info("Calling build-service: GET /api/builds/{}", id);
        return restClient.get()
                .uri("/api/builds/{id}", id)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        (req, res) -> {
                            throw new EntityNotFoundException("Build with ID " + id + " not found");
                        })
                .body(BuildClientResponse.class);
    }
}
