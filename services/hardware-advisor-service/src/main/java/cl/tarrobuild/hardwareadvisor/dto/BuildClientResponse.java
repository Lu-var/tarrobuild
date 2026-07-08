package cl.tarrobuild.hardwareadvisor.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BuildClientResponse(
        Long id,
        Long userId,
        String name,
        String status,
        LocalDateTime createdAt,
        List<BuildItemClientResponse> items
) {}
