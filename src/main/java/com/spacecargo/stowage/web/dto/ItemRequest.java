package com.spacecargo.stowage.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/** Payload to register a cargo item (before placement). */
public record ItemRequest(
        @NotBlank String itemId,
        @NotBlank String name,
        @Positive double width,
        @Positive double depth,
        @Positive double height,
        @PositiveOrZero Double mass,
        @Min(0) @Max(100) int priority,
        LocalDate expiryDate,
        @Positive Integer usageLimit,
        String preferredZone,
        String preferredContainerId) {
}
