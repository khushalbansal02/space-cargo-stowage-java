package com.spacecargo.stowage.domain.geometry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An axis-aligned size (width × depth × height) in a container's coordinate frame.
 *
 * <p>Width runs along X, depth along Y (Y = 0 is the open face the crew reaches
 * through), and height along Z. Immutable value object.
 */
public record Dimensions(double width, double depth, double height) {

    private static final double EPSILON = 1e-3;

    public Dimensions {
        if (width <= 0 || depth <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Dimensions must be strictly positive: " + width + "x" + depth + "x" + height);
        }
    }

    /** Volume of the box. */
    public double volume() {
        return width * depth * height;
    }

    /** True if this box fits inside {@code other} in this exact orientation. */
    public boolean fitsWithin(Dimensions other) {
        return width <= other.width + EPSILON
                && depth <= other.depth + EPSILON
                && height <= other.height + EPSILON;
    }

    /**
     * The distinct axis-aligned orientations produced by the 6 permutations of the
     * three sides. Cubes collapse to one entry; boxes with two equal sides to three.
     */
    public List<Dimensions> orientations() {
        double[][] perms = {
                {width, depth, height},
                {width, height, depth},
                {depth, width, height},
                {depth, height, width},
                {height, width, depth},
                {height, depth, width},
        };
        Map<String, Dimensions> unique = new LinkedHashMap<>();
        for (double[] p : perms) {
            String key = String.format("%.3f_%.3f_%.3f", p[0], p[1], p[2]);
            unique.putIfAbsent(key, new Dimensions(p[0], p[1], p[2]));
        }
        return new ArrayList<>(unique.values());
    }
}
