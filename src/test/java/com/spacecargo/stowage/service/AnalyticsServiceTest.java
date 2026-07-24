package com.spacecargo.stowage.service;

import com.spacecargo.stowage.domain.Container;
import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.geometry.BoundingBox;
import com.spacecargo.stowage.repository.ContainerRepository;
import com.spacecargo.stowage.repository.ItemRepository;
import com.spacecargo.stowage.web.dto.MetricsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AnalyticsServiceTest {

    @Autowired AnalyticsService analyticsService;
    @Autowired ContainerRepository containers;
    @Autowired ItemRepository items;

    @BeforeEach
    void clean() {
        items.deleteAll();
        containers.deleteAll();
    }

    private Item stowed(String id, BoundingBox box) {
        Item item = new Item();
        item.setItemId(id);
        item.setName(id);
        item.setDimensionW(box.w());
        item.setDimensionD(box.d());
        item.setDimensionH(box.h());
        item.setPriority(50);
        item.stow("C1", box);
        return item;
    }

    @Test
    void computesUtilizationFromPlacedVolume() {
        // 10x10x10 container = 1000 capacity; one 2x2x2 item = 8 occupied → 0.8%.
        containers.save(new Container("C1", "Lab", 10, 10, 10));
        items.save(stowed("I1", new BoundingBox(0, 0, 0, 2, 2, 2)));

        MetricsResponse m = analyticsService.computeMetrics();

        assertThat(m.containerCount()).isEqualTo(1);
        assertThat(m.totalCapacity()).isEqualTo(1000.0);
        assertThat(m.occupiedVolume()).isEqualTo(8.0);
        assertThat(m.utilizationPercent()).isEqualTo(0.8);
        assertThat(m.containers()).singleElement()
                .satisfies(c -> assertThat(c.itemCount()).isEqualTo(1));
    }

    @Test
    void averageRetrievalStepsReflectsObstructions() {
        containers.save(new Container("C1", "Lab", 10, 10, 10));
        // Target behind a blocker in the same X/Z column: target needs 1 removal, the
        // blocker needs 0 → average over two items = 0.5.
        items.save(stowed("TARGET", new BoundingBox(0, 5, 0, 2, 2, 2)));
        items.save(stowed("BLOCKER", new BoundingBox(0, 0, 0, 2, 2, 2)));

        MetricsResponse m = analyticsService.computeMetrics();

        assertThat(m.averageRetrievalSteps()).isEqualTo(0.5);
    }

    @Test
    void reportsStatusBreakdownAndCounts() {
        containers.save(new Container("C1", "Lab", 10, 10, 10));
        items.save(stowed("I1", new BoundingBox(0, 0, 0, 2, 2, 2)));

        MetricsResponse m = analyticsService.computeMetrics();

        assertThat(m.itemCount()).isEqualTo(1);
        assertThat(m.itemsByStatus()).containsEntry("STOWED", 1L);
    }
}
