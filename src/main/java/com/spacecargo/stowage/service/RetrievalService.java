package com.spacecargo.stowage.service;

import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.ItemStatus;
import com.spacecargo.stowage.exception.NotFoundException;
import com.spacecargo.stowage.repository.ItemRepository;
import com.spacecargo.stowage.retrieval.PlacedItem;
import com.spacecargo.stowage.retrieval.RetrievalPlanner;
import com.spacecargo.stowage.retrieval.RetrievalSearchResult;
import com.spacecargo.stowage.retrieval.RetrievalStep;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Locates items and confirms their retrieval by the crew.
 */
@Service
public class RetrievalService {

    private final ItemRepository items;
    private final AuditService audit;

    public RetrievalService(ItemRepository items, AuditService audit) {
        this.items = items;
        this.audit = audit;
    }

    /**
     * Finds an item by id (preferred) or name and, if it is stowed, computes the
     * ordered plan to reach it.
     */
    @Transactional(readOnly = true)
    public Optional<RetrievalSearchResult> search(String itemId, String itemName) {
        Optional<Item> match = resolve(itemId, itemName);
        if (match.isEmpty()) {
            return Optional.empty();
        }
        Item item = match.get();
        List<RetrievalStep> steps = item.isStowed() ? planFor(item) : List.of();
        return Optional.of(new RetrievalSearchResult(item, steps));
    }

    private List<RetrievalStep> planFor(Item item) {
        PlacedItem target = new PlacedItem(item.getItemId(), item.getName(), item.stowedBox());
        List<PlacedItem> others = items
                .findByContainerIdAndStatus(item.getContainerId(), ItemStatus.STOWED)
                .stream()
                .filter(i -> !i.getItemId().equals(item.getItemId()))
                .map(i -> new PlacedItem(i.getItemId(), i.getName(), i.stowedBox()))
                .toList();
        return RetrievalPlanner.plan(target, others);
    }

    /**
     * Confirms a crew retrieval: consumes one use, updates status, and frees the
     * item's location. Runs in a single transaction so the read-modify-write of
     * {@code remainingUses} cannot race with a concurrent retrieval.
     *
     * @return the item after the update
     */
    @Transactional
    public Item confirmRetrieval(String itemId, String userId) {
        Item item = items.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item '" + itemId + "' not found."));

        if (item.getStatus() != ItemStatus.STOWED) {
            throw new IllegalStateException(
                    "Item '" + itemId + "' is not stowed (status=" + item.getStatus() + ").");
        }

        boolean usageLimited = item.getUsageLimit() != null;
        Integer newRemaining = item.getRemainingUses();
        ItemStatus newStatus = ItemStatus.RETRIEVED;

        if (usageLimited) {
            int remaining = newRemaining == null ? 0 : newRemaining;
            remaining = Math.max(0, remaining - 1);
            newRemaining = remaining;
            if (remaining == 0) {
                newStatus = ItemStatus.CONSUMED;
            }
        }

        item.setRemainingUses(newRemaining);
        item.setStatus(newStatus);
        item.clearPlacement();
        items.save(item);

        audit.record(userId, "RETRIEVAL", itemId, Map.of(
                "newStatus", newStatus.name(),
                "remainingUses", String.valueOf(newRemaining)));

        return item;
    }

    private Optional<Item> resolve(String itemId, String itemName) {
        if (itemId != null && !itemId.isBlank()) {
            return items.findById(itemId.trim());
        }
        if (itemName != null && !itemName.isBlank()) {
            return items.findFirstByNameContainingIgnoreCase(itemName.trim());
        }
        throw new IllegalArgumentException("Provide either itemId or itemName.");
    }
}
