package com.spacecargo.stowage.waste;

import java.util.List;

/**
 * A proposed manifest of waste items to move into an undocking container for
 * disposal, bounded by an optional mass budget.
 *
 * @param undockingContainerId container the waste will be consolidated into
 * @param entries              waste items selected for return, heaviest first
 * @param totalMass            summed mass of the selected items (kg)
 * @param totalVolume          summed as-placed volume of the selected items
 */
public record WasteReturnPlan(
        String undockingContainerId,
        List<WasteEntry> entries,
        double totalMass,
        double totalVolume) {

    public int itemCount() {
        return entries.size();
    }
}
