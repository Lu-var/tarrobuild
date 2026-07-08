package cl.tarrobuild.apigateway.config;

import cl.tarrobuild.apigateway.filter.CorrelationIdFilter;
import cl.tarrobuild.apigateway.filter.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static cl.tarrobuild.apigateway.config.PublicPaths.*;
import static cl.tarrobuild.apigateway.exception.ApiError.writeJson;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorrelationIdFilter correlationIdFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, CorrelationIdFilter correlationIdFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.correlationIdFilter = correlationIdFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http){
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                .requestMatchers(pathStartsWith(AUTH_PREFIXES.stream().map(p -> p + "/").toArray(String[]::new))).permitAll()
                .requestMatchers(methodAndPath(HttpMethod.GET, PRODUCT_PREFIXES.stream().map(p -> p + "/").toArray(String[]::new))).permitAll()
                .requestMatchers(methodAndPath(HttpMethod.GET, CATEGORY_PREFIXES.stream().map(p -> p + "/").toArray(String[]::new))).permitAll()
                .requestMatchers(pathStartsWith(COMPAT_CHECK_ENDPOINTS.toArray(String[]::new))).permitAll()
                .requestMatchers(pathStartsWith(HEALTH_ENDPOINTS.toArray(String[]::new))).permitAll()
                .requestMatchers(pathStartsWith("/api/builds/", "/api/v1/builds/")).hasAnyRole("USER", "ADMIN")
                .requestMatchers(pathStartsWith("/api/estimate/", "/api/v1/estimate/")).hasAnyRole("USER", "ADMIN")
                .requestMatchers(pathStartsWith("/api/recommendations/", "/api/v1/recommendations/")).hasAnyRole("USER", "ADMIN")
                .requestMatchers(pathStartsWith("/api/notifications/", "/api/v1/notifications/")).hasAnyRole("USER", "ADMIN")
                .requestMatchers(pathStartsWith("/api/products/", "/api/v1/products/")).hasRole("ADMIN")
                .requestMatchers(pathStartsWith("/api/categories/", "/api/v1/categories/")).hasRole("ADMIN")
                .requestMatchers(pathStartsWith("/api/compatibility/", "/api/v1/compatibility/")).hasRole("ADMIN")
                .requestMatchers(pathStartsWith("/api/providers/", "/api/v1/providers/")).hasRole("ADMIN")
                .requestMatchers(pathStartsWith("/api/users/", "/api/v1/users/")).hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(correlationIdFilter, SecurityContextHolderFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        writeJson(response, "Authentication required");
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        writeJson(response, "Access denied");
                    })
            );


        return http.build();
    }

    private static RequestMatcher pathStartsWith(String... prefixes) {
        return request -> {
            String path = request.getRequestURI();
            for (String prefix : prefixes) {
                String normalized = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
                if (path.equals(normalized) || path.startsWith(normalized + "/")) {
                    return true;
                }
            }
            return false;
        };
    }

    private static RequestMatcher methodAndPath(HttpMethod method, String... paths) {
        return request -> request.getMethod().equalsIgnoreCase(method.name())
                && pathStartsWith(paths).matches(request);
    }
}
