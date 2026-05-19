package cl.tarrobuild.apigateway.filter;

import cl.tarrobuild.apigateway.client.AuthRestClient;
import cl.tarrobuild.apigateway.dto.AuthClientResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static cl.tarrobuild.apigateway.exception.ApiError.writeJson;

@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthRestClient authRestClient;

    public JwtAuthFilter(AuthRestClient authRestClient) {
        this.authRestClient = authRestClient;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || (request.getMethod().equals("GET") && (path.startsWith("/api/products") || path.startsWith("/api/categories")))
                || (request.getMethod().equals("POST") && path.equals("/api/compatibility/check"))
                || path.equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (token.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            try {
                AuthClientResponse authResponse = authRestClient.validateToken(token);
                List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + authResponse.role())
                );
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        authResponse.email(),
                        null,
                        authorities
                    );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Token validated for user: {}", authResponse.email());

            } catch (HttpClientErrorException e) {
                log.warn("Token rejected by auth-service: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                writeJson(response, "Invalid or expired token");
            }
            catch (ResourceAccessException e) {
                log.error("Auth-service unreachable: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                writeJson(response, "Authentication service unavailable");
            }
        }

        filterChain.doFilter(request, response);
    }
}
