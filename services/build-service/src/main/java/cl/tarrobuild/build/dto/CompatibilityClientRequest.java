package cl.tarrobuild.build.dto;

import java.util.List;

public record CompatibilityClientRequest(
        Long buildId,
        List<Long> productIds
) {}
