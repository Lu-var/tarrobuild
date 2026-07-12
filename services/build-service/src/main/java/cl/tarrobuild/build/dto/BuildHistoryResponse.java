package cl.tarrobuild.build.dto;

public record BuildHistoryResponse(
    Long id,
    Long buildId,
    String data,
    String changedAt
) {}
