package com.spacecargo.stowage.simulate;

import java.time.LocalDate;
import java.util.List;

/**
 * How far to advance the simulation and what the crew uses each day.
 *
 * <p>Exactly one of {@code numOfDays} or {@code toDate} must be supplied.
 *
 * @param numOfDays        number of days to advance (mutually exclusive with toDate)
 * @param toDate           advance until this date (mutually exclusive with numOfDays)
 * @param itemsUsedPerDay  items the crew consumes each day
 */
public record SimulationRequest(
        Integer numOfDays,
        LocalDate toDate,
        List<ItemUsage> itemsUsedPerDay) {

    public record ItemUsage(String itemId, int quantity) {
    }

    public List<ItemUsage> usageOrEmpty() {
        return itemsUsedPerDay == null ? List.of() : itemsUsedPerDay;
    }
}
