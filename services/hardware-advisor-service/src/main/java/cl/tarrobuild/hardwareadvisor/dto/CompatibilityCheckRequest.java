package cl.tarrobuild.hardwareadvisor.dto;

import java.util.List;

public record CompatibilityCheckRequest(
        Long buildId,
        List<Long> productIds
) {}
