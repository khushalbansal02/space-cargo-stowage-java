package com.spacecargo.stowage.service;

import com.spacecargo.stowage.domain.Container;
import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.ItemStatus;
import com.spacecargo.stowage.placement.PlacementOutcome;
import com.spacecargo.stowage.repository.ContainerRepository;
import com.spacecargo.stowage.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PlacementServiceTest {

    @Autowired PlacementService placementService;
    @Autowired ContainerRepository containers;
    @Autowired ItemRepository items;

    @BeforeEach
    void clean() {
        items.deleteAll();
        containers.deleteAll();
    }

    private Item availableItem(String id, int priority, String preferredZone) {
        Item item = new Item();
        item.setItemId(id);
        item.setName(id);
        item.setDimensionW(2);
        item.setDimensionD(2);
        item.setDimensionH(2);
        item.setPriority(priority);
        item.setPreferredZone(preferredZone);
        item.setStatus(ItemStatus.AVAILABLE);
        return item;
    }

    @Test
    void placesAnItemAndPersistsItAsStowed() {
        containers.save(new Container("C1", "Crew", 10, 10, 10));
        items.save(availableItem("I1", 50, null));

        List<PlacementOutcome> outcomes = placementService.placeItems(List.of("I1"), "tester");

        assertThat(outcomes).singleElement().satisfies(o -> {
            assertThat(o.placed()).isTrue();
            assertThat(o.containerId()).isEqualTo("C1");
        });

        Item stored = items.findById("I1").orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(ItemStatus.STOWED);
        assertThat(stored.getContainerId()).isEqualTo("C1");
        assertThat(stored.isStowed()).isTrue();
    }

    @Test
    void honoursZonePreferenceWhenSpaceExists() {
        containers.save(new Container("A", "Airlock", 10, 10, 10));
        containers.save(new Container("B", "Lab", 10, 10, 10));
        items.save(availableItem("I1", 90, "Lab"));

        List<PlacementOutcome> outcomes = placementService.placeItems(List.of("I1"), "tester");

        assertThat(outcomes).singleElement().satisfies(o -> {
            assertThat(o.containerId()).isEqualTo("B");
            assertThat(o.preferred()).isTrue();
        });
    }

    @Test
    void fallsBackToAnotherContainerWhenPreferredZoneIsFull() {
        // Tiny preferred container fits exactly one 2x2x2 item; second must spill over.
        containers.save(new Container("SMALL", "Lab", 2, 2, 2));
        containers.save(new Container("BIG", "Storage", 10, 10, 10));
        items.save(availableItem("first", 80, "Lab"));
        items.save(availableItem("second", 80, "Lab"));

        List<PlacementOutcome> outcomes = placementService.placeItems(List.of("first", "second"), "tester");

        assertThat(outcomes).allSatisfy(o -> assertThat(o.placed()).isTrue());
        assertThat(outcomes).anySatisfy(o -> {
            assertThat(o.containerId()).isEqualTo("SMALL");
            assertThat(o.preferred()).isTrue();
        });
        assertThat(outcomes).anySatisfy(o -> {
            assertThat(o.containerId()).isEqualTo("BIG");
            assertThat(o.preferred()).isFalse();
        });
    }

    @Test
    void defaultsToPlacingAllAvailableItemsWhenNoIdsGiven() {
        containers.save(new Container("C1", "Crew", 10, 10, 10));
        items.save(availableItem("I1", 30, null));
        items.save(availableItem("I2", 70, null));

        List<PlacementOutcome> outcomes = placementService.placeItems(null, "tester");

        assertThat(outcomes).hasSize(2);
        assertThat(outcomes).allSatisfy(o -> assertThat(o.placed()).isTrue());
    }
}
