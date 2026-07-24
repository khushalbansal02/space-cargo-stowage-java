package com.spacecargo.stowage.placement;

import com.spacecargo.stowage.domain.geometry.BoundingBox;

/**
 * Scores a candidate placement by how easy the item will be to reach later.
 * <b>Lower is better.</b>
 *
 * <p>The three axes are weighted so they form a strict lexicographic order —
 * depth dominates height, which dominates width:
 * <pre>
 *   score = y·10^6 + z·10^3 + x
 * </pre>
 * Depth (Y, distance from the open face) matters most because a shallow item is
 * reachable without moving anything in front of it. Among equally shallow spots we
 * prefer lower ones (smaller Z), and among those the left-most (smaller X) to pack
 * tightly and deterministically.
 */
public final class AccessibilityScorer {

    public static final double WEIGHT_Y = 1_000_000.0;
    public static final double WEIGHT_Z = 1_000.0;
    public static final double WEIGHT_X = 1.0;

    private AccessibilityScorer() {
    }

    public static double score(BoundingBox box) {
        return box.y() * WEIGHT_Y + box.z() * WEIGHT_Z + box.x() * WEIGHT_X;
    }
}
