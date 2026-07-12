package cl.tarrobuild.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.prefixPath;
import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.stripPrefix;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayRoutesConfig {

    private RouterFunction<ServerResponse> v1Route(String id, String apiPath, String lbUrl) {
        return route(id + "-v1")
                .route(path("/api/v1/" + apiPath + "/**"), http())
                .before(uri(lbUrl))
                .filter(stripPrefix(2))
                .filter(prefixPath("/api"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes() {
        return route("auth-service")
                .route(path("/api/auth/**"), http())
                .before(uri("lb://auth-service"))
                .build()
                .and(v1Route("auth-service", "auth", "lb://auth-service"))
                .and(route("user-service")
                        .route(path("/api/users/**"), http())
                        .before(uri("lb://user-service"))
                        .build())
                .and(v1Route("user-service", "users", "lb://user-service"))
                .and(route("product-service")
                        .route(path("/api/products/**"), http())
                        .before(uri("lb://product-service"))
                        .build())
                .and(v1Route("product-service", "products", "lb://product-service"))
                .and(route("category-service")
                        .route(path("/api/categories/**"), http())
                        .before(uri("lb://category-service"))
                        .build())
                .and(v1Route("category-service", "categories", "lb://category-service"))
                .and(route("compatibility-service")
                        .route(path("/api/compatibility/**"), http())
                        .before(uri("lb://compatibility-service"))
                        .build())
                .and(v1Route("compatibility-service", "compatibility", "lb://compatibility-service"))
                .and(route("provider-service")
                        .route(path("/api/providers/**"), http())
                        .before(uri("lb://provider-service"))
                        .build())
                .and(v1Route("provider-service", "providers", "lb://provider-service"))
                .and(route("build-service")
                        .route(path("/api/builds/**"), http())
                        .before(uri("lb://build-service"))
                        .build())
                .and(v1Route("build-service", "builds", "lb://build-service"))
                .and(route("estimate-service")
                        .route(path("/api/estimate/**"), http())
                        .before(uri("lb://estimate-service"))
                        .build())
                .and(v1Route("estimate-service", "estimate", "lb://estimate-service"))
                .and(route("hardware-advisor")
                        .route(path("/api/recommendations/**"), http())
                        .before(uri("lb://hardware-advisor-service"))
                        .build())
                .and(v1Route("hardware-advisor", "recommendations", "lb://hardware-advisor-service"))
                .and(route("notification-service")
                        .route(path("/api/notifications/**"), http())
                        .before(uri("lb://notification-service"))
                        .build())
                .and(v1Route("notification-service", "notifications", "lb://notification-service"));
    }
}
