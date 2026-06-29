package cl.tarrobuild.apigateway.filter;

import cl.tarrobuild.apigateway.client.AuthRestClient;
import cl.tarrobuild.apigateway.dto.AuthClientResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

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
        return path.equals("/api/v1/auth/login") || path.equals("/api/auth/login")
                || path.equals("/api/v1/auth/register") || path.equals("/api/auth/register")
                || (request.getMethod().equals("GET") && (path.startsWith("/api/v1/products") || path.startsWith("/api/products")
                    || path.startsWith("/api/v1/categories") || path.startsWith("/api/categories")))
                || (request.getMethod().equals("POST") && (path.equals("/api/v1/compatibility/check") || path.equals("/api/compatibility/check")))
                || path.equals("/api/v1/actuator/health") || path.equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
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

                request = new IdentityHeaderWrapper(request, authResponse);

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

    private static class IdentityHeaderWrapper extends HttpServletRequestWrapper {

        private static final Set<String> IDENTITY_HEADERS = Set.of(
                "X-User-Id", "X-User-Email", "X-User-Role"
        );

        private final AuthClientResponse auth;

        IdentityHeaderWrapper(HttpServletRequest request, AuthClientResponse auth) {
            super(request);
            this.auth = auth;
        }

        @Override
        public String getHeader(String name) {
            if ("X-User-Id".equals(name)) {
                return auth.userId() != null ? auth.userId().toString() : super.getHeader(name);
            }
            if ("X-User-Email".equals(name)) {
                return auth.email() != null ? auth.email() : super.getHeader(name);
            }
            if ("X-User-Role".equals(name)) {
                return auth.role() != null ? auth.role() : super.getHeader(name);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (IDENTITY_HEADERS.contains(name)) {
                String value = getHeader(name);
                return Collections.enumeration(value != null ? List.of(value) : Collections.emptyList());
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new HashSet<>(Collections.list(super.getHeaderNames()));
            names.addAll(IDENTITY_HEADERS);
            return Collections.enumeration(names);
        }
    }
}
