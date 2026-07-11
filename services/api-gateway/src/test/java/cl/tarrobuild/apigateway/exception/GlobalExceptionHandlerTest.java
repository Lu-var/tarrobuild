package cl.tarrobuild.apigateway.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(handler, "environment", "production");
    }

    @Test
    void handleIllegalArgumentShouldReturn400() {
        ResponseEntity<?> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Invalid parameter"));

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertInstanceOf(ApiError.class, response.getBody());
        assertEquals("Invalid parameter", ((ApiError) response.getBody()).message());
    }

    @Test
    void handleBadCredentialsShouldReturn401() {
        ResponseEntity<?> response = handler.handleBadCredentials(
                new BadCredentialsException("Wrong password"));

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertInstanceOf(ApiError.class, response.getBody());
        assertEquals("Invalid credentials", ((ApiError) response.getBody()).message());
    }

    @Test
    void handleAccessDeniedShouldReturn403() {
        ResponseEntity<?> response = handler.handleAccessDenied(
                new AccessDeniedException("Not allowed"));

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertInstanceOf(ApiError.class, response.getBody());
        assertEquals("Access denied", ((ApiError) response.getBody()).message());
    }

    @Test
    void handleResourceAccessShouldReturn503() {
        ResponseEntity<?> response = handler.handleResourceAccess(
                new ResourceAccessException("Connection refused"));

        assertEquals(503, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertInstanceOf(ApiError.class, response.getBody());
        assertEquals("Downstream service unavailable", ((ApiError) response.getBody()).message());
    }

    @Test
    void handleGenericExceptionShouldReturn500() {
        ResponseEntity<?> response = handler.handleGeneric(
                new RuntimeException("Something went wrong"));

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertInstanceOf(ApiError.class, response.getBody());
        assertEquals("Internal server error", ((ApiError) response.getBody()).message());
    }

    @Test
    void shouldNotIncludeDetailsInProduction() {
        ResponseEntity<?> response = handler.handleIllegalArgument(
                new IllegalArgumentException("error"));

        ApiError error = (ApiError) response.getBody();
        assertNotNull(error);
        assertNull(error.details());
    }

    @Test
    void shouldIncludeDetailsInDevelopment() {
        ReflectionTestUtils.setField(handler, "environment", "development");

        ResponseEntity<?> response = handler.handleIllegalArgument(
                new IllegalArgumentException("error"));

        ApiError error = (ApiError) response.getBody();
        assertNotNull(error);
        assertNotNull(error.details());
    }

    @Test
    void shouldAlwaysIncludeTimestamp() {
        ResponseEntity<?> response = handler.handleGeneric(new RuntimeException("error"));

        ApiError error = (ApiError) response.getBody();
        assertNotNull(error);
        assertNotNull(error.timestamp());
    }
}
