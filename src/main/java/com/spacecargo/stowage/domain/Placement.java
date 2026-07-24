package com.spacecargo.stowage.domain;

import com.spacecargo.stowage.domain.geometry.BoundingBox;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Where an item physically sits inside its container: the origin corner
 * {@code (x, y, z)} plus the as-placed dimensions (which already account for
 * whichever of the six orientations was chosen).
 *
 * <p>Embedded into {@link Item}; all fields are null when the item is not stowed.
 */
@Embeddable
public class Placement {

    @Column(name = "position_x")
    private Double positionX;

    @Column(name = "position_y")
    private Double positionY;

    @Column(name = "position_z")
    private Double positionZ;

    @Column(name = "placed_dimension_w")
    private Double placedDimensionW;

    @Column(name = "placed_dimension_d")
    private Double placedDimensionD;

    @Column(name = "placed_dimension_h")
    private Double placedDimensionH;

    protected Placement() {
    }

    public Placement(Double positionX, Double positionY, Double positionZ,
                     Double placedDimensionW, Double placedDimensionD, Double placedDimensionH) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.positionZ = positionZ;
        this.placedDimensionW = placedDimensionW;
        this.placedDimensionD = placedDimensionD;
        this.placedDimensionH = placedDimensionH;
    }

    public static Placement fromBox(BoundingBox box) {
        return new Placement(box.x(), box.y(), box.z(), box.w(), box.d(), box.h());
    }

    /** True when every coordinate/dimension is populated. */
    public boolean isComplete() {
        return positionX != null && positionY != null && positionZ != null
                && placedDimensionW != null && placedDimensionD != null && placedDimensionH != null;
    }

    public BoundingBox toBox() {
        if (!isComplete()) {
            throw new IllegalStateException("Placement is incomplete; cannot build a bounding box");
        }
        return new BoundingBox(positionX, positionY, positionZ,
                placedDimensionW, placedDimensionD, placedDimensionH);
    }

    public Double getPositionX() { return positionX; }
    public Double getPositionY() { return positionY; }
    public Double getPositionZ() { return positionZ; }
    public Double getPlacedDimensionW() { return placedDimensionW; }
    public Double getPlacedDimensionD() { return placedDimensionD; }
    public Double getPlacedDimensionH() { return placedDimensionH; }
}
