package com.spacecargo.stowage.service;

import com.spacecargo.stowage.domain.Container;
import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.ItemStatus;
import com.spacecargo.stowage.domain.geometry.BoundingBox;
import com.spacecargo.stowage.repository.ContainerRepository;
import com.spacecargo.stowage.repository.ItemRepository;
import com.spacecargo.stowage.retrieval.RetrievalSearchResult;
import com.spacecargo.stowage.retrieval.RetrievalStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class RetrievalServiceTest {

    @Autowired RetrievalService retrievalService;
    @Autowired ContainerRepository containers;
    @Autowired ItemRepository items;

    @BeforeEach
    void clean() {
        items.deleteAll();
        containers.deleteAll();
    }

    private Item stowedItem(String id, BoundingBox box, Integer usageLimit, Integer remaining) {
        Item item = new Item();
        item.setItemId(id);
        item.setName(id);
        item.setDimensionW(box.w());
        item.setDimensionD(box.d());
        item.setDimensionH(box.h());
        item.setPriority(50);
        item.setUsageLimit(usageLimit);
        item.setRemainingUses(remaining);
        item.stow("C1", box);
        return item;
    }

    @Test
    void searchReturnsObstructionPlanForAStowedItem() {
        containers.save(new Container("C1", "Lab", 10, 10, 10));
        items.save(stowedItem("TARGET", new BoundingBox(0, 5, 0, 2, 2, 2), null, null));
        items.save(stowedItem("BLOCKER", new BoundingBox(0, 0, 0, 2, 2, 2), null, null));

        Optional<RetrievalSearchResult> result = retrievalService.search("TARGET", null);

        assertThat(result).isPresent();
        assertThat(result.get().steps()).hasSize(2);
        assertThat(result.get().steps().get(0).action()).isEqualTo(RetrievalStep.Action.REMOVE);
        assertThat(result.get().steps().get(0).itemId()).isEqualTo("BLOCKER");
    }

    @Test
    void searchByNameMatchesCaseInsensitively() {
        containers.save(new Container("C1", "Lab", 10, 10, 10));
        Item item = stowedItem("X1", new BoundingBox(0, 0, 0, 2, 2, 2), null, null);
        item.setName("Oxygen Cylinder");
        items.save(item);

        Optional<RetrievalSearchResult> result = retrievalService.search(null, "oxygen");

        assertThat(result).isPresent();
        assertThat(result.get().item().getItemId()).isEqualTo("X1");
    }

    @Test
    void confirmingRetrievalConsumesAUseAndFreesLocation() {
        containers.save(new Container("C1", "Lab", 10, 10, 10));
        items.save(stowedItem("U", new BoundingBox(0, 0, 0, 2, 2, 2), 3, 3));

        Item after = retrievalService.confirmRetrieval("U", "crew");

        assertThat(after.getRemainingUses()).isEqualTo(2);
        assertThat(after.getStatus()).isEqualTo(ItemStatus.RETRIEVED);
        assertThat(after.getContainerId()).isNull();
        assertThat(after.getPlacement()).isNull();
    }

    @Test
    void lastUseFlipsTheItemToConsumed() {
        containers.save(new Container("C1", "Lab", 10, 10, 10));
        items.save(stowedItem("LAST", new BoundingBox(0, 0, 0, 2, 2, 2), 1, 1));

        Item after = retrievalService.confirmRetrieval("LAST", "crew");

        assertThat(after.getRemainingUses()).isZero();
        assertThat(after.getStatus()).isEqualTo(ItemStatus.CONSUMED);
    }

    @Test
    void retrievingANonStowedItemIsRejected() {
        Item available = new Item();
        available.setItemId("A");
        available.setName("A");
        available.setDimensionW(1);
        available.setDimensionD(1);
        available.setDimensionH(1);
        available.setPriority(10);
        available.setStatus(ItemStatus.AVAILABLE);
        items.save(available);

        assertThatThrownBy(() -> retrievalService.confirmRetrieval("A", "crew"))
                .isInstanceOf(IllegalStateException.class);
    }
}
