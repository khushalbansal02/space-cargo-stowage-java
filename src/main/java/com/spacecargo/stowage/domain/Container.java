package com.spacecargo.stowage.domain;

import com.spacecargo.stowage.domain.geometry.Dimensions;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A storage container fixed to a station zone, with an interior volume the
 * placement engine packs items into.
 */
@Entity
@Table(name = "containers")
public class Container {

    @Id
    @Column(name = "container_id")
    private String containerId;

    @Column(nullable = false)
    private String zone;

    @Column(nullable = false)
    private double width;

    @Column(nullable = false)
    private double depth;

    @Column(nullable = false)
    private double height;

    @Column(name = "created_at", updatable = false, insertable = false)
    private Instant createdAt;

    protected Container() {
    }

    public Container(String containerId, String zone, double width, double depth, double height) {
        this.containerId = containerId;
        this.zone = zone;
        this.width = width;
        this.depth = depth;
        this.height = height;
    }

    /** Interior size as a geometry value object for the placement engine. */
    public Dimensions dimensions() {
        return new Dimensions(width, depth, height);
    }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }

    public double getDepth() { return depth; }
    public void setDepth(double depth) { this.depth = depth; }

    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    public Instant getCreatedAt() { return createdAt; }
}
