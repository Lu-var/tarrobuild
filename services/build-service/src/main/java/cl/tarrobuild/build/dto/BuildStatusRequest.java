package cl.tarrobuild.build.dto;

import cl.tarrobuild.build.model.BuildStatus;
import jakarta.validation.constraints.NotNull;

public record BuildStatusRequest(
        @NotNull(message = "Status cannot be null")
        BuildStatus status
) {}
