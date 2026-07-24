package com.spacecargo.stowage.service;

import com.spacecargo.stowage.domain.Container;
import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.ItemStatus;
import com.spacecargo.stowage.repository.ContainerRepository;
import com.spacecargo.stowage.repository.ItemRepository;
import com.spacecargo.stowage.retrieval.PlacedItem;
import com.spacecargo.stowage.retrieval.RetrievalPlanner;
import com.spacecargo.stowage.web.dto.MetricsResponse;
import com.spacecargo.stowage.web.dto.MetricsResponse.ContainerUtilization;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Computes station-wide operational analytics — container utilization, average
 * retrieval effort, and outstanding waste — and publishes the headline figures as
 * Micrometer gauges so they show up under {@code /actuator/metrics}.
 */
@Service
public class AnalyticsService {

    private final ContainerRepository containers;
    private final ItemRepository items;
    private final WasteService waste;
    private final MeterRegistry meterRegistry;

    public AnalyticsService(ContainerRepository containers, ItemRepository items,
                            WasteService waste, MeterRegistry meterRegistry) {
        this.containers = containers;
        this.items = items;
        this.waste = waste;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void bindGauges() {
        Gauge.builder("cargo.utilization.percent", this, AnalyticsService::liveUtilizationPercent)
                .description("Overall container volume utilisation (%)")
                .register(meterRegistry);
        Gauge.builder("cargo.items.stowed", items, r -> r.countByStatus(ItemStatus.STOWED))
                .description("Number of currently stowed items")
                .register(meterRegistry);
    }

    @Transactional(readOnly = true)
    public MetricsResponse computeMetrics() {
        List<Container> allContainers = containers.findAll();

        double totalCapacity = 0.0;
        double totalOccupied = 0.0;
        List<ContainerUtilization> perContainer = new ArrayList<>();
        double stepsSum = 0.0;
        int stowedItems = 0;

        for (Container container : allContainers) {
            List<Item> stowed = items.findByContainerIdAndStatus(container.getContainerId(), ItemStatus.STOWED);
            double capacity = container.dimensions().volume();
            double occupied = stowed.stream().mapToDouble(this::placedVolume).sum();
            totalCapacity += capacity;
            totalOccupied += occupied;

            perContainer.add(new ContainerUtilization(
                    container.getContainerId(), container.getZone(),
                    capacity, occupied, percent(occupied, capacity), stowed.size()));

            stepsSum += totalObstructionSteps(stowed);
            stowedItems += stowed.size();
        }

        Map<String, Long> byStatus = countByStatus();
        double avgSteps = stowedItems == 0 ? 0.0 : round(stepsSum / stowedItems);
        double wasteMass = waste.identifyWaste().stream()
                .mapToDouble(e -> e.item().getMass() == null ? 0.0 : e.item().getMass())
                .sum();
        int wasteCount = waste.identifyWaste().size();

        return new MetricsResponse(
                allContainers.size(),
                items.count(),
                byStatus,
                round(totalCapacity),
                round(totalOccupied),
                percent(totalOccupied, totalCapacity),
                avgSteps,
                wasteCount,
                round(wasteMass),
                perContainer);
    }

    /** Sum of obstructions across every item in one container (each item's plan). */
    private long totalObstructionSteps(List<Item> stowed) {
        List<PlacedItem> placed = stowed.stream()
                .map(i -> new PlacedItem(i.getItemId(), i.getName(), i.stowedBox()))
                .toList();
        long total = 0;
        for (PlacedItem target : placed) {
            total += RetrievalPlanner.obstructionCount(RetrievalPlanner.plan(target, placed));
        }
        return total;
    }

    private Map<String, Long> countByStatus() {
        Map<ItemStatus, Long> counts = new EnumMap<>(ItemStatus.class);
        for (ItemStatus status : ItemStatus.values()) {
            counts.put(status, items.countByStatus(status));
        }
        Map<String, Long> result = new java.util.LinkedHashMap<>();
        counts.forEach((k, v) -> result.put(k.name(), v));
        return result;
    }

    private double liveUtilizationPercent() {
        double capacity = 0.0;
        double occupied = 0.0;
        for (Container container : containers.findAll()) {
            capacity += container.dimensions().volume();
            occupied += items.findByContainerIdAndStatus(container.getContainerId(), ItemStatus.STOWED)
                    .stream().mapToDouble(this::placedVolume).sum();
        }
        return percent(occupied, capacity);
    }

    private double placedVolume(Item item) {
        return item.stowedBox().w() * item.stowedBox().d() * item.stowedBox().h();
    }

    private double percent(double part, double whole) {
        return whole <= 0 ? 0.0 : round(part / whole * 100.0);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
