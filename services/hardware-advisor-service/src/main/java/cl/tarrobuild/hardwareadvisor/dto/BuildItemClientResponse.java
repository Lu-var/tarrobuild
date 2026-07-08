package cl.tarrobuild.hardwareadvisor.dto;

public record BuildItemClientResponse(
        Long id,
        Long buildId,
        Long productId,
        Integer quantity
) {}
