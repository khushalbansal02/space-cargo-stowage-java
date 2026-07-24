package com.spacecargo.stowage.domain.geometry;

/**
 * An axis-aligned box placed at an origin {@code (x, y, z)} with a size
 * {@code (w, d, h)}, expressed in a container's coordinate frame.
 *
 * <p>Convention: Y = 0 is the open face of the container. A smaller Y therefore
 * means "closer to the crew" — the basis for both the accessibility score used by
 * the placement engine and the obstruction check used during retrieval.
 */
public record BoundingBox(double x, double y, double z, double w, double d, double h) {

    private static final double EPSILON = 1e-3;

    public static BoundingBox of(double x, double y, double z, Dimensions size) {
        return new BoundingBox(x, y, z, size.width(), size.depth(), size.height());
    }

    public double maxX() { return x + w; }
    public double maxY() { return y + d; }
    public double maxZ() { return z + h; }

    /** True if this box shares any interior volume with {@code other} (full 3D overlap). */
    public boolean overlaps(BoundingBox other) {
        boolean separateX = maxX() <= other.x + EPSILON || other.maxX() <= x + EPSILON;
        boolean separateY = maxY() <= other.y + EPSILON || other.maxY() <= y + EPSILON;
        boolean separateZ = maxZ() <= other.z + EPSILON || other.maxZ() <= z + EPSILON;
        return !(separateX || separateY || separateZ);
    }

    /**
     * True if this box overlaps {@code other} when projected onto the X–Z (front-face)
     * plane, ignoring depth. Used to decide whether an item in front actually blocks
     * the line of sight to a target item behind it.
     */
    public boolean overlapsFrontFace(BoundingBox other) {
        boolean overlapX = x < other.maxX() - EPSILON && maxX() > other.x + EPSILON;
        boolean overlapZ = z < other.maxZ() - EPSILON && maxZ() > other.z + EPSILON;
        return overlapX && overlapZ;
    }

    /** True if {@code other} sits strictly in front of this box (nearer the open face). */
    public boolean isInFrontOf(BoundingBox other) {
        return this.y < other.y - EPSILON;
    }

    /** True if this box lies fully within a container of the given size. */
    public boolean fitsInContainer(Dimensions container) {
        return x >= -EPSILON && y >= -EPSILON && z >= -EPSILON
                && maxX() <= container.width() + EPSILON
                && maxY() <= container.depth() + EPSILON
                && maxZ() <= container.height() + EPSILON;
    }
}
