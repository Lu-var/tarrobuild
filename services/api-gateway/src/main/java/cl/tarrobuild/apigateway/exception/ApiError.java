package cl.tarrobuild.apigateway.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String message,
        String details,
        String timestamp
)
{
    public static void writeJson(HttpServletResponse response, String message) throws IOException {
        response.getWriter().write(
                """
                {"message":"%s","timestamp":"%s"}
                """.formatted(message, LocalDateTime.now().toString())
        );
    }
}
