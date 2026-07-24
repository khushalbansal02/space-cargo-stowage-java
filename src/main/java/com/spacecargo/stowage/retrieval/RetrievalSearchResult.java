package com.spacecargo.stowage.retrieval;

import com.spacecargo.stowage.domain.Item;

import java.util.List;

/**
 * Outcome of a search: the matched item and, when it is stowed, the ordered plan
 * to retrieve it. {@code steps} is empty for items that are not currently stowed.
 */
public record RetrievalSearchResult(Item item, List<RetrievalStep> steps) {
}
