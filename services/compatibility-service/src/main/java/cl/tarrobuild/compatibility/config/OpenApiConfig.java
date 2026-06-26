package cl.tarrobuild.compatibility.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Compatibility Service API")
                        .version("1.0")
                        .description("Documentación del microservicio de validación de compatibilidad de TarroBuild"));
    }
}