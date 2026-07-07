package cl.tarrobuild.provider.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TarroBuild - Provider API")
                        .description("Servicio de gestión de proveedores y stock externo")
                        .version("1.0")
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")));
    }
}