package com.spacecargo.stowage.placement;

import com.spacecargo.stowage.domain.geometry.BoundingBox;
import com.spacecargo.stowage.domain.geometry.Dimensions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mutable in-memory view of one container during a placement run: its interior
 * size plus the boxes currently occupying it. Lets the engine try and commit many
 * placements without touching the database between candidates.
 */
public class ContainerSpace {

    private final String containerId;
    private final String zone;
    private final Dimensions dimensions;
    private final List<BoundingBox> occupied;

    public ContainerSpace(String containerId, String zone, Dimensions dimensions,
                          List<BoundingBox> initiallyOccupied) {
        this.containerId = containerId;
        this.zone = zone;
        this.dimensions = dimensions;
        this.occupied = new ArrayList<>(initiallyOccupied);
    }

    /** Best free spot for an item of the given size, or empty if it does not fit. */
    public Optional<BoundingBox> findSpot(Dimensions item) {
        return PlacementHeuristic.findSpot(item, dimensions, occupied);
    }

    /** Record a box as occupied (call after committing a placement here). */
    public void occupy(BoundingBox box) {
        occupied.add(box);
    }

    public String getContainerId() { return containerId; }
    public String getZone() { return zone; }
    public Dimensions getDimensions() { return dimensions; }
    public List<BoundingBox> getOccupied() { return occupied; }
}
