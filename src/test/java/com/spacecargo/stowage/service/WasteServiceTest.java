package com.spacecargo.stowage.service;

import com.spacecargo.stowage.domain.Container;
import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.ItemStatus;
import com.spacecargo.stowage.domain.geometry.BoundingBox;
import com.spacecargo.stowage.repository.ContainerRepository;
import com.spacecargo.stowage.repository.ItemRepository;
import com.spacecargo.stowage.waste.WasteReturnPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WasteServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 24);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-07-24T00:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired WasteService wasteService;
    @Autowired ContainerRepository containers;
    @Autowired ItemRepository items;

    @BeforeEach
    void clean() {
        items.deleteAll();
        containers.deleteAll();
    }

    private Item stowed(String id, double mass) {
        Item item = new Item();
        item.setItemId(id);
        item.setName(id);
        item.setDimensionW(2);
        item.setDimensionD(2);
        item.setDimensionH(2);
        item.setPriority(50);
        item.setMass(mass);
        item.stow("C1", new BoundingBox(0, 0, 0, 2, 2, 2));
        return item;
    }

    @Test
    void identifiesExpiredAndConsumedButNotHealthyItems() {
        containers.save(new Container("C1", "Lab", 10, 10, 10));

        Item expired = stowed("EXP", 5);
        expired.setExpiryDate(TODAY.minusDays(1));
        items.save(expired);

        Item consumed = stowed("CON", 3);
        consumed.setUsageLimit(2);
        consumed.setRemainingUses(0);
        items.save(consumed);

        Item healthy = stowed("OK", 4);
        healthy.setExpiryDate(TODAY.plusDays(10));
        items.save(healthy);

        assertThat(wasteService.identifyWaste())
                .extracting(e -> e.item().getItemId())
                .containsExactlyInAnyOrder("EXP", "CON");
    }

    @Test
    void returnPlanRespectsTheMassBudget() {
        containers.save(new Container("C1", "Lab", 10, 10, 10));
        containers.save(new Container("UNDOCK", "Airlock", 10, 10, 10));

        Item heavy = stowed("HEAVY", 8);
        heavy.setStatus(ItemStatus.EXPIRED);
        items.save(heavy);

        Item light = stowed("LIGHT", 3);
        light.setStatus(ItemStatus.EXPIRED);
        items.save(light);

        // Budget of 5kg: the 8kg item is skipped, the 3kg item fits.
        WasteReturnPlan plan = wasteService.planReturn("UNDOCK", 5.0);

        assertThat(plan.itemCount()).isEqualTo(1);
        assertThat(plan.entries().get(0).item().getItemId()).isEqualTo("LIGHT");
        assertThat(plan.totalMass()).isEqualTo(3.0);
    }

    @Test
    void completeUndockingDisposesItemsAndFreesLocation() {
        containers.save(new Container("C1", "Lab", 10, 10, 10));
        Item expired = stowed("EXP", 5);
        expired.setStatus(ItemStatus.EXPIRED);
        items.save(expired);

        int disposed = wasteService.completeUndocking(List.of("EXP"), "crew");

        assertThat(disposed).isEqualTo(1);
        Item after = items.findById("EXP").orElseThrow();
        assertThat(after.getStatus()).isEqualTo(ItemStatus.DISPOSED);
        assertThat(after.getContainerId()).isNull();
    }
}
