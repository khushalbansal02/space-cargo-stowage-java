package com.spacecargo.stowage.service;

import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.ItemStatus;
import com.spacecargo.stowage.exception.NotFoundException;
import com.spacecargo.stowage.repository.ContainerRepository;
import com.spacecargo.stowage.repository.ItemRepository;
import com.spacecargo.stowage.waste.WasteEntry;
import com.spacecargo.stowage.waste.WasteEntry.WasteReason;
import com.spacecargo.stowage.waste.WasteReturnPlan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Identifies waste (expired or fully-consumed items), proposes a return manifest
 * bounded by an optional mass budget, and completes disposal on undocking.
 */
@Service
public class WasteService {

    private final ItemRepository items;
    private final ContainerRepository containers;
    private final AuditService audit;
    private final Clock clock;

    public WasteService(ItemRepository items, ContainerRepository containers,
                        AuditService audit, Clock clock) {
        this.items = items;
        this.containers = containers;
        this.audit = audit;
        this.clock = clock;
    }

    /** Every item currently classifiable as waste, with the reason. */
    @Transactional(readOnly = true)
    public List<WasteEntry> identifyWaste() {
        LocalDate today = LocalDate.now(clock);
        List<WasteEntry> waste = new ArrayList<>();
        for (Item item : items.findAll()) {
            classify(item, today).ifPresent(reason -> waste.add(new WasteEntry(item, reason)));
        }
        return waste;
    }

    private Optional<WasteReason> classify(Item item, LocalDate today) {
        if (item.getStatus() == ItemStatus.EXPIRED) {
            return Optional.of(WasteReason.EXPIRED);
        }
        if (item.getStatus() == ItemStatus.CONSUMED) {
            return Optional.of(WasteReason.CONSUMED);
        }
        if (item.getStatus() == ItemStatus.STOWED) {
            if (item.getExpiryDate() != null && item.getExpiryDate().isBefore(today)) {
                return Optional.of(WasteReason.EXPIRED);
            }
            if (item.getUsageLimit() != null && item.getRemainingUses() != null
                    && item.getRemainingUses() <= 0) {
                return Optional.of(WasteReason.CONSUMED);
            }
        }
        return Optional.empty();
    }

    /**
     * Selects waste items to consolidate into {@code undockingContainerId}, heaviest
     * first, until the optional {@code maxMass} budget is exhausted.
     *
     * @param maxMass mass budget in kg, or null for no limit
     */
    @Transactional(readOnly = true)
    public WasteReturnPlan planReturn(String undockingContainerId, Double maxMass) {
        if (!containers.existsById(undockingContainerId)) {
            throw new NotFoundException("Undocking container '" + undockingContainerId + "' not found.");
        }

        List<WasteEntry> candidates = new ArrayList<>(identifyWaste());
        candidates.sort(Comparator.comparingDouble((WasteEntry e) -> mass(e.item())).reversed());

        List<WasteEntry> selected = new ArrayList<>();
        double totalMass = 0.0;
        double totalVolume = 0.0;
        for (WasteEntry entry : candidates) {
            double m = mass(entry.item());
            if (maxMass != null && totalMass + m > maxMass) {
                continue; // skip items that would blow the budget, keep trying lighter ones
            }
            selected.add(entry);
            totalMass += m;
            totalVolume += placedVolume(entry.item());
        }
        return new WasteReturnPlan(undockingContainerId, selected, totalMass, totalVolume);
    }

    /**
     * Marks the given items disposed and frees their location — the physical
     * undocking has happened.
     *
     * @return the number of items disposed
     */
    @Transactional
    public int completeUndocking(List<String> itemIds, String userId) {
        int disposed = 0;
        for (String id : itemIds) {
            Optional<Item> found = items.findById(id);
            if (found.isEmpty()) {
                continue;
            }
            Item item = found.get();
            item.setStatus(ItemStatus.DISPOSED);
            item.clearPlacement();
            items.save(item);
            audit.record(userId, "WASTE_UNDOCK", id, Map.of("status", "DISPOSED"));
            disposed++;
        }
        return disposed;
    }

    private double mass(Item item) {
        return item.getMass() == null ? 0.0 : item.getMass();
    }

    private double placedVolume(Item item) {
        return item.isStowed() ? item.stowedBox().w() * item.stowedBox().d() * item.stowedBox().h()
                : item.dimensions().volume();
    }
}
