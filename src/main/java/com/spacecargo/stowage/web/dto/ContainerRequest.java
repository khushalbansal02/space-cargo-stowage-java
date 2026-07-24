package com.spacecargo.stowage.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Payload to create a container. */
public record ContainerRequest(
        @NotBlank String containerId,
        @NotBlank String zone,
        @Positive double width,
        @Positive double depth,
        @Positive double height) {
}
