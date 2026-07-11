package cl.tarrobuild.apigateway.filter;

import cl.tarrobuild.apigateway.client.AuthRestClient;
import cl.tarrobuild.apigateway.dto.AuthClientResponse;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private AuthRestClient authRestClient;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        authRestClient = mock(AuthRestClient.class);
        filter = new JwtAuthFilter(authRestClient);
    }

    @Test
    void shouldReturn401WhenNoAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/builds");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void shouldReturn401WhenAuthorizationHeaderHasNoBearerPrefix() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/builds");
        request.addHeader("Authorization", "Basic abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void shouldReturn401WhenBearerTokenIsBlank() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/builds");
        request.addHeader("Authorization", "Bearer ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void shouldPassThroughWhenTokenIsValid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/builds");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        AuthClientResponse authResponse = new AuthClientResponse(1L, "valid-token", "user@test.com", "USER");
        when(authRestClient.validateToken("valid-token")).thenReturn(authResponse);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(any(), eq(response));
    }

    @Test
    void shouldSetAuthenticationWithUserRole() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/builds");
        request.addHeader("Authorization", "Bearer user-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        AuthClientResponse authResponse = new AuthClientResponse(1L, "user-token", "user@test.com", "USER");
        when(authRestClient.validateToken("user-token")).thenReturn(authResponse);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(argThat(req -> {
            jakarta.servlet.http.HttpServletRequest wrapped =
                    (jakarta.servlet.http.HttpServletRequest) req;
            return "user@test.com".equals(wrapped.getHeader("X-User-Email"))
                    && "USER".equals(wrapped.getHeader("X-User-Role"));
        }), eq(response));
    }

    @Test
    void shouldSetAuthenticationWithAdminRole() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/products");
        request.addHeader("Authorization", "Bearer admin-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        AuthClientResponse authResponse = new AuthClientResponse(2L, "admin-token", "admin@test.com", "ADMIN");
        when(authRestClient.validateToken("admin-token")).thenReturn(authResponse);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(argThat(req -> {
            jakarta.servlet.http.HttpServletRequest wrapped =
                    (jakarta.servlet.http.HttpServletRequest) req;
            return "ADMIN".equals(wrapped.getHeader("X-User-Role"));
        }), eq(response));
    }

    @Test
    void shouldReturn401WhenTokenIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/builds");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(authRestClient.validateToken("invalid-token"))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.UNAUTHORIZED, "Unauthorized",
                        HttpHeaders.EMPTY, new byte[0], java.nio.charset.StandardCharsets.UTF_8));

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void shouldReturn503WhenAuthServiceIsUnavailable() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/builds");
        request.addHeader("Authorization", "Bearer some-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(authRestClient.validateToken("some-token"))
                .thenThrow(new ResourceAccessException("Connection refused"));

        filter.doFilterInternal(request, response, chain);

        assertEquals(503, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void shouldNotFilterAuthLoginPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilterV1AuthLoginPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilterAuthRegisterPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilterGetProducts() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilterGetV1Products() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilterGetCategories() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/categories");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilterHealthEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilterPostCompatCheck() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/compatibility/check");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldFilterBuildsPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/builds");
        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void shouldFilterPostProducts() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/products");
        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void shouldFilterEstimatePath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/estimate");
        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void shouldFilterUsersPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        assertFalse(filter.shouldNotFilter(request));
    }
}
