package com.spacecargo.stowage.simulate;

import java.time.LocalDate;
import java.util.List;

/**
 * Summary of what changed over a simulated time span.
 *
 * @param newDate  the simulated date after advancing
 * @param used     items whose uses were consumed, with the amount and remaining uses
 * @param expired  items that passed their expiry date during the span
 * @param consumed items whose uses hit zero during the span
 */
public record SimulationResult(
        LocalDate newDate,
        List<UsageChange> used,
        List<ItemRef> expired,
        List<ItemRef> consumed) {

    public record UsageChange(String itemId, String name, int quantityUsed, Integer remainingUses) {
    }

    public record ItemRef(String itemId, String name) {
    }
}
