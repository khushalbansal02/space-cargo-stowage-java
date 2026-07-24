package com.spacecargo.stowage.service;

import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.ItemStatus;
import com.spacecargo.stowage.domain.SimulationState;
import com.spacecargo.stowage.repository.ItemRepository;
import com.spacecargo.stowage.repository.SimulationStateRepository;
import com.spacecargo.stowage.simulate.SimulationRequest;
import com.spacecargo.stowage.simulate.SimulationRequest.ItemUsage;
import com.spacecargo.stowage.simulate.SimulationResult;
import com.spacecargo.stowage.simulate.SimulationResult.ItemRef;
import com.spacecargo.stowage.simulate.SimulationResult.UsageChange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Advances the simulated station clock day by day, applying daily item usage and
 * flipping items to EXPIRED (expiry reached) or CONSUMED (uses exhausted).
 */
@Service
public class SimulationService {

    private final SimulationStateRepository state;
    private final ItemRepository items;
    private final AuditService audit;

    public SimulationService(SimulationStateRepository state, ItemRepository items, AuditService audit) {
        this.state = state;
        this.items = items;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public LocalDate currentDate() {
        return singleton().getSimDate();
    }

    @Transactional
    public SimulationResult simulate(SimulationRequest request, String userId) {
        SimulationState sim = singleton();
        LocalDate current = sim.getSimDate();
        int days = resolveDays(request, current);

        List<UsageChange> used = new ArrayList<>();
        List<ItemRef> expired = new ArrayList<>();
        List<ItemRef> consumed = new ArrayList<>();

        for (int day = 0; day < days; day++) {
            current = current.plusDays(1);
            applyExpiry(current, expired);
            applyUsage(request.usageOrEmpty(), used, consumed);
        }

        sim.setSimDate(current);
        state.save(sim);

        audit.record(userId, "SIMULATION", null, Map.of(
                "days", String.valueOf(days),
                "newDate", current.toString(),
                "expired", String.valueOf(expired.size()),
                "consumed", String.valueOf(consumed.size())));

        return new SimulationResult(current, used, expired, consumed);
    }

    /** Mark any stowed item whose expiry date has arrived as EXPIRED (location retained). */
    private void applyExpiry(LocalDate current, List<ItemRef> expired) {
        for (Item item : items.findByStatus(ItemStatus.STOWED)) {
            if (item.getExpiryDate() != null && !item.getExpiryDate().isAfter(current)) {
                item.setStatus(ItemStatus.EXPIRED);
                items.save(item);
                expired.add(new ItemRef(item.getItemId(), item.getName()));
            }
        }
    }

    private void applyUsage(List<ItemUsage> usages, List<UsageChange> used, List<ItemRef> consumed) {
        for (ItemUsage usage : usages) {
            items.findById(usage.itemId())
                    .filter(i -> i.getStatus() == ItemStatus.STOWED)
                    .ifPresent(item -> applyUsageToItem(item, usage.quantity(), used, consumed));
        }
    }

    private void applyUsageToItem(Item item, int quantity, List<UsageChange> used, List<ItemRef> consumed) {
        int qty = Math.max(1, quantity);
        Integer remaining = item.getRemainingUses();
        int actuallyUsed = qty;

        if (item.getUsageLimit() != null && remaining != null) {
            actuallyUsed = Math.min(qty, remaining);
            remaining = remaining - actuallyUsed;
            item.setRemainingUses(remaining);
            if (remaining <= 0) {
                item.setStatus(ItemStatus.CONSUMED);
                consumed.add(new ItemRef(item.getItemId(), item.getName()));
            }
        }
        items.save(item);
        used.add(new UsageChange(item.getItemId(), item.getName(), actuallyUsed, item.getRemainingUses()));
    }

    private int resolveDays(SimulationRequest request, LocalDate current) {
        boolean hasDays = request.numOfDays() != null;
        boolean hasTarget = request.toDate() != null;
        if (hasDays == hasTarget) {
            throw new IllegalArgumentException("Provide exactly one of numOfDays or toDate.");
        }
        if (hasDays) {
            if (request.numOfDays() <= 0) {
                throw new IllegalArgumentException("numOfDays must be positive.");
            }
            return request.numOfDays();
        }
        if (!request.toDate().isAfter(current)) {
            throw new IllegalArgumentException("toDate must be after the current simulation date " + current + ".");
        }
        return (int) ChronoUnit.DAYS.between(current, request.toDate());
    }

    private SimulationState singleton() {
        return state.findById(SimulationState.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Simulation state not initialised."));
    }
}
