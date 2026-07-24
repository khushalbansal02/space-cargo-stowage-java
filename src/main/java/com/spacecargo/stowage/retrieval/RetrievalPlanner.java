package com.spacecargo.stowage.retrieval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Computes the minimum ordered sequence of removals needed to reach a target item.
 * Pure and stateless.
 *
 * <p>An item obstructs the target when it sits <em>in front</em> of it (a smaller
 * depth Y, i.e. nearer the open face) <em>and</em> its front-face (X–Z) projection
 * overlaps the target's — meaning the crew cannot slide the target out without first
 * moving it. Obstructions are removed nearest-first (ascending depth).
 */
public final class RetrievalPlanner {

    private RetrievalPlanner() {
    }

    /**
     * @param target the item to retrieve
     * @param others every other stowed item in the same container
     * @return ordered steps: each blocking item as a {@code REMOVE}, then the target as {@code RETRIEVE}
     */
    public static List<RetrievalStep> plan(PlacedItem target, List<PlacedItem> others) {
        List<PlacedItem> blockers = new ArrayList<>();
        for (PlacedItem other : others) {
            if (other.itemId().equals(target.itemId())) {
                continue;
            }
            boolean inFront = other.box().isInFrontOf(target.box());
            boolean overlaps = target.box().overlapsFrontFace(other.box());
            if (inFront && overlaps) {
                blockers.add(other);
            }
        }
        blockers.sort(Comparator.comparingDouble(b -> b.box().y()));

        List<RetrievalStep> steps = new ArrayList<>();
        int step = 1;
        for (PlacedItem blocker : blockers) {
            steps.add(new RetrievalStep(step++, RetrievalStep.Action.REMOVE,
                    blocker.itemId(), blocker.name()));
        }
        steps.add(new RetrievalStep(step, RetrievalStep.Action.RETRIEVE,
                target.itemId(), target.name()));
        return steps;
    }

    /** Number of items that must be moved before the target is reachable. */
    public static long obstructionCount(List<RetrievalStep> plan) {
        return plan.stream().filter(s -> s.action() == RetrievalStep.Action.REMOVE).count();
    }
}
