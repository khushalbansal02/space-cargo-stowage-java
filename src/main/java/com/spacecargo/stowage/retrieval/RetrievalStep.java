package com.spacecargo.stowage.retrieval;

/**
 * One step in a retrieval plan: either temporarily {@code REMOVE} an obstructing
 * item, or finally {@code RETRIEVE} the target.
 *
 * @param step     1-based order in which to perform the steps
 * @param action   {@link Action#REMOVE} or {@link Action#RETRIEVE}
 * @param itemId   the item to act on
 * @param itemName its display name
 */
public record RetrievalStep(int step, Action action, String itemId, String itemName) {

    public enum Action {
        REMOVE,
        RETRIEVE
    }
}
