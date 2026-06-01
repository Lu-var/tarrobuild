package cl.tarrobuild.estimate.dto;

import java.util.List;

public record BuildClientResponse(
        Long id,
        Long userId,
        String name,
        String status,
        List<BuildItemClientResponse> items
) {}
