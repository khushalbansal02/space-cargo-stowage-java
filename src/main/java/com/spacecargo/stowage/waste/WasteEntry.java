package com.spacecargo.stowage.waste;

import com.spacecargo.stowage.domain.Item;

/** A waste item together with why it is considered waste. */
public record WasteEntry(Item item, WasteReason reason) {

    public enum WasteReason {
        EXPIRED,
        CONSUMED
    }
}
