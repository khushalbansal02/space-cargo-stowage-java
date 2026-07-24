package com.spacecargo.stowage.retrieval;

import com.spacecargo.stowage.domain.geometry.BoundingBox;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalPlannerTest {

    private PlacedItem item(String id, double x, double y, double z) {
        return new PlacedItem(id, id, new BoundingBox(x, y, z, 2, 2, 2));
    }

    @Test
    void anUnobstructedItemIsRetrievedInASingleStep() {
        PlacedItem target = item("T", 0, 0, 0);
        List<RetrievalStep> plan = RetrievalPlanner.plan(target, List.of());

        assertThat(plan).singleElement().satisfies(s -> {
            assertThat(s.action()).isEqualTo(RetrievalStep.Action.RETRIEVE);
            assertThat(s.itemId()).isEqualTo("T");
            assertThat(s.step()).isEqualTo(1);
        });
    }

    @Test
    void itemDirectlyInFrontMustBeRemovedFirst() {
        PlacedItem target = item("T", 0, 5, 0);
        PlacedItem blocker = item("B", 0, 0, 0); // same X/Z column, nearer the opening

        List<RetrievalStep> plan = RetrievalPlanner.plan(target, List.of(blocker));

        assertThat(plan).hasSize(2);
        assertThat(plan.get(0).action()).isEqualTo(RetrievalStep.Action.REMOVE);
        assertThat(plan.get(0).itemId()).isEqualTo("B");
        assertThat(plan.get(1).action()).isEqualTo(RetrievalStep.Action.RETRIEVE);
    }

    @Test
    void itemInFrontButInADifferentColumnDoesNotObstruct() {
        PlacedItem target = item("T", 0, 5, 0);
        PlacedItem beside = item("B", 5, 0, 0); // in front by depth, but different X column

        List<RetrievalStep> plan = RetrievalPlanner.plan(target, List.of(beside));

        assertThat(plan).singleElement()
                .satisfies(s -> assertThat(s.action()).isEqualTo(RetrievalStep.Action.RETRIEVE));
    }

    @Test
    void itemBehindTheTargetNeverObstructs() {
        PlacedItem target = item("T", 0, 0, 0);
        PlacedItem behind = item("B", 0, 5, 0);

        List<RetrievalStep> plan = RetrievalPlanner.plan(target, List.of(behind));

        assertThat(RetrievalPlanner.obstructionCount(plan)).isZero();
    }

    @Test
    void multipleBlockersAreRemovedNearestFirst() {
        PlacedItem target = item("T", 0, 9, 0);
        PlacedItem far = item("FAR", 0, 6, 0);
        PlacedItem near = item("NEAR", 0, 2, 0);

        List<RetrievalStep> plan = RetrievalPlanner.plan(target, List.of(far, near));

        assertThat(plan).hasSize(3);
        assertThat(plan.get(0).itemId()).isEqualTo("NEAR"); // smaller Y removed first
        assertThat(plan.get(1).itemId()).isEqualTo("FAR");
        assertThat(plan.get(2).itemId()).isEqualTo("T");
    }
}
