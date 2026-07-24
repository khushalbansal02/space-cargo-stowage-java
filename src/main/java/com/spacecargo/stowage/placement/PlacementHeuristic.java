package com.spacecargo.stowage.placement;

import com.spacecargo.stowage.domain.geometry.BoundingBox;
import com.spacecargo.stowage.domain.geometry.Dimensions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Finds the most accessible free spot for one item inside one container, given the
 * items already placed there. Pure and stateless — no persistence, no Spring — so
 * the core packing logic can be unit-tested in isolation.
 *
 * <h2>Approach</h2>
 * A deterministic extreme-point / surface heuristic:
 * <ol>
 *   <li>The set of <em>candidate anchor points</em> is the container origin plus the
 *       right, top, and back corners of every already-placed box (clamped to the
 *       container).</li>
 *   <li>Each of the item's distinct orientations is tried at each candidate point.</li>
 *   <li>Positions that leave the container or collide with an existing box are
 *       discarded; the survivor with the best {@link AccessibilityScorer} score wins.</li>
 * </ol>
 */
public final class PlacementHeuristic {

    private static final double EPSILON = 1e-3;

    private PlacementHeuristic() {
    }

    /**
     * @param item      intrinsic (unrotated) size of the item to place
     * @param container interior size of the target container
     * @param existing  boxes already occupying the container
     * @return the best free {@link BoundingBox}, or empty if the item does not fit anywhere
     */
    public static Optional<BoundingBox> findSpot(Dimensions item,
                                                 Dimensions container,
                                                 List<BoundingBox> existing) {
        List<double[]> anchors = candidateAnchors(container, existing);

        BoundingBox best = null;
        double bestScore = Double.POSITIVE_INFINITY;

        for (Dimensions orientation : item.orientations()) {
            if (!orientation.fitsWithin(container)) {
                continue;
            }
            for (double[] anchor : anchors) {
                BoundingBox candidate = BoundingBox.of(anchor[0], anchor[1], anchor[2], orientation);
                if (!candidate.fitsInContainer(container)) {
                    continue;
                }
                if (collides(candidate, existing)) {
                    continue;
                }
                double score = AccessibilityScorer.score(candidate);
                if (score < bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean collides(BoundingBox candidate, List<BoundingBox> existing) {
        for (BoundingBox other : existing) {
            if (candidate.overlaps(other)) {
                return true;
            }
        }
        return false;
    }

    /** Origin plus the three "growth" corners of each existing box, de-duplicated. */
    private static List<double[]> candidateAnchors(Dimensions container, List<BoundingBox> existing) {
        Map<String, double[]> unique = new LinkedHashMap<>();
        addAnchor(unique, 0.0, 0.0, 0.0);

        for (BoundingBox box : existing) {
            addAnchorIfInside(unique, container, box.maxX(), box.y(), box.z()); // right
            addAnchorIfInside(unique, container, box.x(), box.maxY(), box.z()); // behind
            addAnchorIfInside(unique, container, box.x(), box.y(), box.maxZ()); // on top
        }
        return new ArrayList<>(unique.values());
    }

    private static void addAnchorIfInside(Map<String, double[]> unique, Dimensions container,
                                          double x, double y, double z) {
        if (x < container.width() + EPSILON
                && y < container.depth() + EPSILON
                && z < container.height() + EPSILON) {
            addAnchor(unique, x, y, z);
        }
    }

    private static void addAnchor(Map<String, double[]> unique, double x, double y, double z) {
        String key = String.format("%.3f_%.3f_%.3f", x, y, z);
        unique.putIfAbsent(key, new double[]{x, y, z});
    }
}
