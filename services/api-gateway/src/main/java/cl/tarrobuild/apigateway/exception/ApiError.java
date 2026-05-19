package cl.tarrobuild.apigateway.exception;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;

public record ApiError(
        String message,
        String details,
        String timestamp
)
{
    public static void writeJson(HttpServletResponse response, String message) throws IOException {
        response.getWriter().write(
                """
                {"message":"%s","details":null,"timestamp":"%s"}
                """.formatted(message, LocalDateTime.now().toString())
        );
    }
}
