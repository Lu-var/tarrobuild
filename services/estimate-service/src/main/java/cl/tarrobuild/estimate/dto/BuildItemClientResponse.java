package cl.tarrobuild.estimate.dto;

public record BuildItemClientResponse(
        Long id,
        Long productId,
        Integer quantity
) {}
