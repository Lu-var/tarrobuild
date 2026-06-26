package cl.tarrobuild.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationAndIdentityFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. Obtener o generar Correlation ID
        String correlationId = request.getHeaders().getFirst("X-Correlation-Id");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // 2. Rescatar los headers de identidad
        String userId = request.getHeaders().getFirst("X-User-Id");
        String userEmail = request.getHeaders().getFirst("X-User-Email");
        String userRole = request.getHeaders().getFirst("X-User-Role");

        // Mutar la request para inyectar de forma segura los headers aguas abajo (downstream)
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Correlation-Id", correlationId)
                .header("X-User-Id", userId != null ? userId : "")
                .header("X-User-Email", userEmail != null ? userEmail : "")
                .header("X-User-Role", userRole != null ? userRole : "")
                .build();

        // Guardar el Correlation ID en los atributos del exchange
        exchange.getAttributes().put("X-Correlation-Id", correlationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -1;
    }
}