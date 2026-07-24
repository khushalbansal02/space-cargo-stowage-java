package com.spacecargo.stowage.placement;

import com.spacecargo.stowage.domain.geometry.BoundingBox;
import com.spacecargo.stowage.domain.geometry.Dimensions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PlacementHeuristicTest {

    private final Dimensions container = new Dimensions(10, 10, 10);

    @Test
    void placesFirstItemAtTheFrontBottomLeftOrigin() {
        Optional<BoundingBox> spot =
                PlacementHeuristic.findSpot(new Dimensions(2, 2, 2), container, List.of());

        assertThat(spot).isPresent();
        BoundingBox box = spot.get();
        assertThat(box.x()).isEqualTo(0.0);
        assertThat(box.y()).isEqualTo(0.0);
        assertThat(box.z()).isEqualTo(0.0);
    }

    @Test
    void returnsEmptyWhenItemCannotFitInAnyOrientation() {
        Optional<BoundingBox> spot =
                PlacementHeuristic.findSpot(new Dimensions(20, 1, 1), container, List.of());
        assertThat(spot).isEmpty();
    }

    @Test
    void rotatesAnItemSoItFits() {
        // 12 x 2 x 2 does not fit as-is (12 > 10) but 2 x 2 x ... no axis is 12; use 8x2x2
        // Here the long side (8) must land on an axis <= 10; every axis is fine, but make
        // the container thin so only one orientation works.
        Dimensions thin = new Dimensions(3, 9, 3);
        Optional<BoundingBox> spot =
                PlacementHeuristic.findSpot(new Dimensions(2, 8, 2), thin, List.of());
        assertThat(spot).isPresent();
        assertThat(spot.get().fitsInContainer(thin)).isTrue();
    }

    @Test
    void secondItemDoesNotOverlapTheFirst() {
        BoundingBox first = new BoundingBox(0, 0, 0, 4, 4, 4);
        Optional<BoundingBox> spot =
                PlacementHeuristic.findSpot(new Dimensions(4, 4, 4), container, List.of(first));

        assertThat(spot).isPresent();
        assertThat(spot.get().overlaps(first)).isFalse();
    }

    @Test
    void prefersTheMostAccessibleSpotAmongCandidates() {
        // One box occupies the front-bottom-left corner. The next 4x4x4 item should be
        // placed at the next-most-accessible anchor: to the right at the same depth/height,
        // i.e. (4,0,0) — not on top (z) or behind (y), which score worse.
        BoundingBox occupied = new BoundingBox(0, 0, 0, 4, 4, 4);
        Optional<BoundingBox> spot =
                PlacementHeuristic.findSpot(new Dimensions(4, 4, 4), container, List.of(occupied));

        assertThat(spot).isPresent();
        BoundingBox box = spot.get();
        assertThat(box.y()).isEqualTo(0.0);   // still at the front
        assertThat(box.z()).isEqualTo(0.0);   // still on the floor
        assertThat(box.x()).isEqualTo(4.0);   // shifted right
    }

    @Test
    void packsManyUnitCubesWithoutAnyOverlap() {
        Dimensions unit = new Dimensions(1, 1, 1);
        java.util.List<BoundingBox> placed = new java.util.ArrayList<>();

        for (int i = 0; i < 50; i++) {
            Optional<BoundingBox> spot = PlacementHeuristic.findSpot(unit, container, placed);
            assertThat(spot).as("cube %d should fit", i).isPresent();
            BoundingBox box = spot.get();
            assertThat(box.fitsInContainer(container)).isTrue();
            for (BoundingBox other : placed) {
                assertThat(box.overlaps(other)).as("cube %d overlaps an earlier one", i).isFalse();
            }
            placed.add(box);
        }
    }
}
