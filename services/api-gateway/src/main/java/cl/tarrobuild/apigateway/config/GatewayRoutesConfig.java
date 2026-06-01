package cl.tarrobuild.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayRoutesConfig {

    @Value("${AUTH_SERVICE_URL:http://localhost:8081}") private String authUrl;
    @Value("${USER_SERVICE_URL:http://localhost:8082}") private String userUrl;
    @Value("${PRODUCT_SERVICE_URL:http://localhost:8083}") private String productUrl;
    @Value("${CATEGORY_SERVICE_URL:http://localhost:8084}") private String categoryUrl;
    @Value("${COMPATIBILITY_SERVICE_URL:http://localhost:8085}") private String compatibilityUrl;
    @Value("${PROVIDER_SERVICE_URL:http://localhost:8086}") private String providerUrl;
    @Value("${BUILD_SERVICE_URL:http://localhost:8087}") private String buildUrl;
    @Value("${ESTIMATE_SERVICE_URL:http://localhost:8088}") private String estimateUrl;
    @Value("${HARDWARE_ADVISOR_SERVICE_URL:http://localhost:8089}") private String advisorUrl;
    @Value("${NOTIFICATION_SERVICE_URL:http://localhost:8090}") private String notificationUrl;

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes() {
        return route("auth-service")
                .route(path("/api/auth/**"), http())
                .before(uri(authUrl))
                .build()
                .and(route("user-service")
                        .route(path("/api/users/**"), http())
                        .before(uri(userUrl))
                        .build())
                .and(route("product-service")
                        .route(path("/api/products/**"), http())
                        .before(uri(productUrl))
                        .build())
                .and(route("category-service")
                        .route(path("/api/categories/**"), http())
                        .before(uri(categoryUrl))
                        .build())
                .and(route("compatibility-service")
                        .route(path("/api/compatibility/**"), http())
                        .before(uri(compatibilityUrl))
                        .build())
                .and(route("provider-service")
                        .route(path("/api/providers/**"), http())
                        .before(uri(providerUrl))
                        .build())
                .and(route("build-service")
                        .route(path("/api/builds/**"), http())
                        .before(uri(buildUrl))
                        .build())
                .and(route("estimate-service")
                        .route(path("/api/estimate/**"), http())
                        .before(uri(estimateUrl))
                        .build())
                .and(route("hardware-advisor")
                        .route(path("/api/recommendations/**"), http())
                        .before(uri(advisorUrl))
                        .build())
                .and(route("notification-service")
                        .route(path("/api/notifications/**"), http())
                        .before(uri(notificationUrl))
                        .build());
    }
}