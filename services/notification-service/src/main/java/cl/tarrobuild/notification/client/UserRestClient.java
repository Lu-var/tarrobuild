package cl.tarrobuild.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${app.services.user-service.url:http://localhost:8081}/api/users")
public interface UserRestClient {

    @GetMapping("/{id}")
    UserResponse getUserById(@PathVariable("id") Long id);

    // Record interno para mapear la respuesta que nos interesa del user-service
    record UserResponse(Long id, String email) {}
}