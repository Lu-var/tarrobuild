package cl.tarrobuild.build.dto;

public record ProductClientResponse(
        Long id,
        String name,
        boolean isActive
) {}
