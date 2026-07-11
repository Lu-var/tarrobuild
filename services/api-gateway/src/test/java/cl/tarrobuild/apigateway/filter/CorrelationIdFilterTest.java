package cl.tarrobuild.apigateway.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Test
    void shouldUseProvidedCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("X-Correlation-Id", "my-custom-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] captured = new String[1];
        FilterChain chain = (req, res) -> captured[0] = MDC.get("correlationId");

        filter.doFilterInternal(request, response, chain);

        assertEquals("my-custom-id", captured[0]);
        assertEquals("my-custom-id", response.getHeader("X-Correlation-Id"));
    }

    @Test
    void shouldGenerateCorrelationIdWhenNotProvided() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] captured = new String[1];
        FilterChain chain = (req, res) -> captured[0] = MDC.get("correlationId");

        filter.doFilterInternal(request, response, chain);

        assertNotNull(captured[0]);
        assertDoesNotThrow(() -> UUID.fromString(captured[0]),
                "Generated correlation ID should be a valid UUID");
        assertEquals(captured[0], response.getHeader("X-Correlation-Id"));
    }

    @Test
    void shouldGenerateNewIdWhenHeaderIsBlank() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("X-Correlation-Id", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] captured = new String[1];
        FilterChain chain = (req, res) -> captured[0] = MDC.get("correlationId");

        filter.doFilterInternal(request, response, chain);

        assertNotNull(captured[0]);
        assertFalse(captured[0].isBlank());
        assertNotEquals("   ", captured[0]);
    }

    @Test
    void shouldClearMdcAfterFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertNull(MDC.get("correlationId"),
                "MDC should be cleared after filter chain completes");
    }

    @Test
    void shouldSetCorrelationIdOnResponseHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("X-Correlation-Id", "response-test-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals("response-test-id", response.getHeader("X-Correlation-Id"));
    }

    @Test
    void shouldPassWrappedRequestToFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("X-Correlation-Id", "wrapped-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final jakarta.servlet.http.HttpServletRequest[] capturedReq = new jakarta.servlet.http.HttpServletRequest[1];
        FilterChain chain = (req, res) -> capturedReq[0] = (jakarta.servlet.http.HttpServletRequest) req;

        filter.doFilterInternal(request, response, chain);

        assertEquals("wrapped-id", capturedReq[0].getHeader("X-Correlation-Id"));
    }
}
