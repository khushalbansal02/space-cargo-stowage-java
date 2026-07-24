package com.spacecargo.stowage.domain;

import com.spacecargo.stowage.domain.geometry.BoundingBox;
import com.spacecargo.stowage.domain.geometry.Dimensions;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A cargo item. Carries its intrinsic (unrotated) size and stowage preferences,
 * and — once packed — its {@link ItemStatus}, current container, and {@link Placement}.
 */
@Entity
@Table(name = "items")
public class Item {

    @Id
    @Column(name = "item_id")
    private String itemId;

    @Column(nullable = false)
    private String name;

    @Column(name = "dimension_w", nullable = false)
    private double dimensionW;

    @Column(name = "dimension_d", nullable = false)
    private double dimensionD;

    @Column(name = "dimension_h", nullable = false)
    private double dimensionH;

    private Double mass;

    @Column(nullable = false)
    private int priority;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "remaining_uses")
    private Integer remainingUses;

    @Column(name = "preferred_zone")
    private String preferredZone;

    @Column(name = "preferred_container_id")
    private String preferredContainerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus status = ItemStatus.AVAILABLE;

    @Column(name = "created_at", updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    /** Container the item is currently stowed in, or null if unplaced. */
    @Column(name = "container_id")
    private String containerId;

    @Embedded
    private Placement placement;

    public Item() {
    }

    @PrePersist
    void onCreate() {
        if (lastUpdated == null) {
            lastUpdated = Instant.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        lastUpdated = Instant.now();
    }

    // --- Domain behaviour ------------------------------------------------

    /** Intrinsic, unrotated size. */
    public Dimensions dimensions() {
        return new Dimensions(dimensionW, dimensionD, dimensionH);
    }

    public boolean isStowed() {
        return status == ItemStatus.STOWED && containerId != null
                && placement != null && placement.isComplete();
    }

    /** The item's occupied volume in its container, or null if not stowed. */
    public BoundingBox stowedBox() {
        return isStowed() ? placement.toBox() : null;
    }

    /** Move the item into a container at the given box and mark it stowed. */
    public void stow(String containerId, BoundingBox box) {
        this.containerId = containerId;
        this.placement = Placement.fromBox(box);
        this.status = ItemStatus.STOWED;
        this.lastUpdated = Instant.now();
    }

    /** Clear the item's location (e.g. on retrieval) without changing its status. */
    public void clearPlacement() {
        this.containerId = null;
        this.placement = null;
        this.lastUpdated = Instant.now();
    }

    // --- Accessors -------------------------------------------------------

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getDimensionW() { return dimensionW; }
    public void setDimensionW(double dimensionW) { this.dimensionW = dimensionW; }

    public double getDimensionD() { return dimensionD; }
    public void setDimensionD(double dimensionD) { this.dimensionD = dimensionD; }

    public double getDimensionH() { return dimensionH; }
    public void setDimensionH(double dimensionH) { this.dimensionH = dimensionH; }

    public Double getMass() { return mass; }
    public void setMass(Double mass) { this.mass = mass; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public Integer getRemainingUses() { return remainingUses; }
    public void setRemainingUses(Integer remainingUses) { this.remainingUses = remainingUses; }

    public String getPreferredZone() { return preferredZone; }
    public void setPreferredZone(String preferredZone) { this.preferredZone = preferredZone; }

    public String getPreferredContainerId() { return preferredContainerId; }
    public void setPreferredContainerId(String preferredContainerId) { this.preferredContainerId = preferredContainerId; }

    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }

    public Placement getPlacement() { return placement; }
    public void setPlacement(Placement placement) { this.placement = placement; }
}
