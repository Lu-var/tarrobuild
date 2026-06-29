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

import java.time.LocalDateTime;

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
                .requestMatchers("/api/auth/**", "/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/v1/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/v1/categories/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/compatibility/check", "/api/v1/compatibility/check").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/api/v1/actuator/health").permitAll()
                .requestMatchers("/api/builds/**", "/api/v1/builds/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/estimate/**", "/api/v1/estimate/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/recommendations/**", "/api/v1/recommendations/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/notifications/**", "/api/v1/notifications/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/products/**", "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers("/api/categories/**", "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers("/api/compatibility/**", "/api/v1/compatibility/**").hasRole("ADMIN")
                .requestMatchers("/api/providers/**", "/api/v1/providers/**").hasRole("ADMIN")
                .requestMatchers("/api/users/**", "/api/v1/users/**").hasRole("ADMIN")
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
}
