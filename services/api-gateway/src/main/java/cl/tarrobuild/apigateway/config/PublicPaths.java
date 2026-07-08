package cl.tarrobuild.apigateway.config;

import java.util.List;

public final class PublicPaths {
    private PublicPaths() {}

    public static final List<String> AUTH_PREFIXES = List.of("/api/auth", "/api/v1/auth");
    public static final List<String> PRODUCT_PREFIXES = List.of("/api/products", "/api/v1/products");
    public static final List<String> CATEGORY_PREFIXES = List.of("/api/categories", "/api/v1/categories");
    public static final List<String> COMPAT_CHECK_ENDPOINTS = List.of(
            "/api/compatibility/check", "/api/v1/compatibility/check"
    );
    public static final List<String> HEALTH_ENDPOINTS = List.of(
            "/actuator/health", "/api/v1/actuator/health"
    );
}
