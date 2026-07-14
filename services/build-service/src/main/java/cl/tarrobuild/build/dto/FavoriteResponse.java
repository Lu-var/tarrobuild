package cl.tarrobuild.build.dto;

public record FavoriteResponse(
    Long id,
    Long buildId,
    String buildName,
    String createdAt
) {}
