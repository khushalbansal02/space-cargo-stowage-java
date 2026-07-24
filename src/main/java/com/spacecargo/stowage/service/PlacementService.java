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

    // Graduated preference penalties. Each tier is separated by far more than the
    // largest possible accessibility score, so preference dominates and the tiers
    // never blur: exact container > same-zone sibling > any other container.
    /** A container in the preferred zone, but not the specifically preferred container. */
    private static final double PENALTY_ZONE_SIBLING = 1_000_000_000.0;      // 1e9
    /** A container matching neither the preferred container nor zone. */
    private static final double PENALTY_NON_PREFERRED = 1_000_000_000_000.0; // 1e12

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
            double penalty = preferencePenalty(item, space, spaces);
            boolean preferred = penalty == 0.0;
            double score = AccessibilityScorer.score(box) + penalty;
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

    /**
     * Score penalty for placing {@code item} in {@code space}, ranking containers by
     * how well they satisfy the item's preference:
     * <ul>
     *   <li>no preference, or the exact preferred container → {@code 0} (best);</li>
     *   <li>a different container in the preferred zone → {@link #PENALTY_ZONE_SIBLING};</li>
     *   <li>anything else → {@link #PENALTY_NON_PREFERRED}.</li>
     * </ul>
     * When only a zone is preferred, a zone match is the top tier ({@code 0}).
     */
    private double preferencePenalty(Item item, ContainerSpace space, Map<String, ContainerSpace> spaces) {
        String prefContainer = blankToNull(item.getPreferredContainerId());
        String prefZone = blankToNull(item.getPreferredZone());

        if (prefContainer == null && prefZone == null) {
            return 0.0; // no preference — any container is equally acceptable
        }

        if (prefContainer != null) {
            if (space.getContainerId().equals(prefContainer)) {
                return 0.0; // exact preferred container always wins
            }
            String zone = prefZone != null ? prefZone : zoneOf(prefContainer, spaces);
            if (zone != null && space.getZone().equals(zone)) {
                return PENALTY_ZONE_SIBLING; // right zone, but not the requested container
            }
            return PENALTY_NON_PREFERRED;
        }

        // Only a zone was requested: matching it is the top tier.
        return space.getZone().equals(prefZone) ? 0.0 : PENALTY_NON_PREFERRED;
    }

    private String zoneOf(String containerId, Map<String, ContainerSpace> spaces) {
        ContainerSpace space = spaces.get(containerId);
        return space == null ? null : space.getZone();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
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
