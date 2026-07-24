package com.spacecargo.stowage.service;

import com.spacecargo.stowage.domain.Container;
import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.ItemStatus;
import com.spacecargo.stowage.domain.geometry.BoundingBox;
import com.spacecargo.stowage.placement.AccessibilityScorer;
import com.spacecargo.stowage.placement.ContainerSpace;
import com.spacecargo.stowage.placement.PlacementOutcome;
import com.spacecargo.stowage.repository.ContainerRepository;
import com.spacecargo.stowage.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates packing a batch of items across all containers.
 *
 * <p>Items are placed highest-priority first. For each item the engine scans every
 * container for its most accessible free spot, then adds a large penalty to spots
 * that fall outside the item's preferred container/zone — so a preferred spot always
 * beats a non-preferred one, while still allowing a fallback when the preference is
 * full. State is tracked in memory ({@link ContainerSpace}) and flushed to the
 * database once per placed item.
 */
@Service
public class PlacementService {

    /** Pushes non-preferred containers behind every preferred option without excluding them. */
    private static final double NON_PREFERRED_PENALTY = 1_000_000_000.0;

    private final ContainerRepository containers;
    private final ItemRepository items;
    private final AuditService audit;

    public PlacementService(ContainerRepository containers, ItemRepository items, AuditService audit) {
        this.containers = containers;
        this.items = items;
        this.audit = audit;
    }

    /**
     * Places the given items (or all {@link ItemStatus#AVAILABLE} items when
     * {@code itemIds} is null/empty) and persists the results.
     */
    @Transactional
    public List<PlacementOutcome> placeItems(List<String> itemIds, String userId) {
        Map<String, ContainerSpace> spaces = buildContainerSpaces();
        if (spaces.isEmpty()) {
            throw new IllegalStateException("No containers available to place items into.");
        }

        List<Item> toPlace = resolveItemsToPlace(itemIds);
        toPlace.sort(Comparator.comparingInt(Item::getPriority).reversed());

        List<PlacementOutcome> outcomes = new ArrayList<>();
        for (Item item : toPlace) {
            outcomes.add(placeOne(item, spaces, userId));
        }
        return outcomes;
    }

    private PlacementOutcome placeOne(Item item, Map<String, ContainerSpace> spaces, String userId) {
        ContainerSpace bestSpace = null;
        BoundingBox bestBox = null;
        boolean bestPreferred = false;
        double bestScore = Double.POSITIVE_INFINITY;

        for (ContainerSpace space : spaces.values()) {
            Optional<BoundingBox> spot = space.findSpot(item.dimensions());
            if (spot.isEmpty()) {
                continue;
            }
            BoundingBox box = spot.get();
            boolean preferred = satisfiesPreference(item, space);
            double score = AccessibilityScorer.score(box) + (preferred ? 0.0 : NON_PREFERRED_PENALTY);
            if (score < bestScore) {
                bestScore = score;
                bestSpace = space;
                bestBox = box;
                bestPreferred = preferred;
            }
        }

        if (bestSpace == null) {
            PlacementOutcome outcome = PlacementOutcome.failed(item.getItemId(),
                    "No free space found in any container.");
            audit.record(userId, "PLACEMENT_FAILED", item.getItemId(),
                    Map.of("reason", outcome.reason()));
            return outcome;
        }

        item.stow(bestSpace.getContainerId(), bestBox);
        items.save(item);
        bestSpace.occupy(bestBox);

        audit.record(userId, "PLACEMENT", item.getItemId(), Map.of(
                "containerId", bestSpace.getContainerId(),
                "preferred", bestPreferred,
                "x", bestBox.x(), "y", bestBox.y(), "z", bestBox.z(),
                "w", bestBox.w(), "d", bestBox.d(), "h", bestBox.h()));

        return PlacementOutcome.placed(item.getItemId(), bestSpace.getContainerId(), bestBox, bestPreferred);
    }

    /** No preference means anything is acceptable; otherwise match container id or zone. */
    private boolean satisfiesPreference(Item item, ContainerSpace space) {
        boolean hasContainerPref = item.getPreferredContainerId() != null;
        boolean hasZonePref = item.getPreferredZone() != null;
        if (!hasContainerPref && !hasZonePref) {
            return true;
        }
        if (hasContainerPref && space.getContainerId().equals(item.getPreferredContainerId())) {
            return true;
        }
        return hasZonePref && space.getZone().equals(item.getPreferredZone());
    }

    private Map<String, ContainerSpace> buildContainerSpaces() {
        Map<String, ContainerSpace> spaces = new LinkedHashMap<>();
        for (Container container : containers.findAll()) {
            List<BoundingBox> occupied = items
                    .findByContainerIdAndStatus(container.getContainerId(), ItemStatus.STOWED)
                    .stream()
                    .map(Item::stowedBox)
                    .toList();
            spaces.put(container.getContainerId(), new ContainerSpace(
                    container.getContainerId(), container.getZone(), container.dimensions(), occupied));
        }
        return spaces;
    }

    private List<Item> resolveItemsToPlace(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return new ArrayList<>(items.findByStatus(ItemStatus.AVAILABLE));
        }
        List<Item> resolved = new ArrayList<>();
        for (String id : itemIds) {
            items.findById(id).ifPresent(resolved::add);
        }
        return resolved;
    }
}
