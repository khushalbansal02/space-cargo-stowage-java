package com.spacecargo.stowage.domain;

/**
 * Lifecycle states of a cargo item.
 *
 * <ul>
 *   <li>{@link #AVAILABLE} — imported but not yet stowed in a container.</li>
 *   <li>{@link #STOWED}    — physically placed at coordinates inside a container.</li>
 *   <li>{@link #RETRIEVED} — pulled out by the crew (no longer occupying space).</li>
 *   <li>{@link #EXPIRED}   — past its expiry date; still occupies space until undocked.</li>
 *   <li>{@link #CONSUMED}  — usage limit reached; treated as waste.</li>
 *   <li>{@link #DISPOSED}  — removed from the station via a waste-return undocking.</li>
 * </ul>
 */
public enum ItemStatus {
    AVAILABLE,
    STOWED,
    RETRIEVED,
    EXPIRED,
    CONSUMED,
    DISPOSED;

    /** True when the item counts as waste that should be scheduled for return. */
    public boolean isWaste() {
        return this == EXPIRED || this == CONSUMED;
    }
}
