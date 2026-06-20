package cl.tarrobuild.compatibility.dto;

public record ProductDTO(
        Long id,
        String name,
        String categoryName,
        String socketType,
        Integer tdp
) {}