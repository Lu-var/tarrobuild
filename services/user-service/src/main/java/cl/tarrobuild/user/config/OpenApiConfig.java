package cl.tarrobuild.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userServiceAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Service API")
                        .description("Gestión de perfiles de usuario — CRUD de usuarios")
                        .version("1.0")
                        .license(new License().name("Apache 2.0")));
    }
}
