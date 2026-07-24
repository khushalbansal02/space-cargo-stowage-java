package com.spacecargo.stowage.domain.geometry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoundingBoxTest {

    @Test
    void overlappingBoxesAreDetected() {
        BoundingBox a = new BoundingBox(0, 0, 0, 2, 2, 2);
        BoundingBox b = new BoundingBox(1, 1, 1, 2, 2, 2);
        assertThat(a.overlaps(b)).isTrue();
        assertThat(b.overlaps(a)).isTrue();
    }

    @Test
    void faceTouchingBoxesDoNotOverlap() {
        BoundingBox a = new BoundingBox(0, 0, 0, 2, 2, 2);
        BoundingBox b = new BoundingBox(2, 0, 0, 2, 2, 2); // shares the x=2 face only
        assertThat(a.overlaps(b)).isFalse();
    }

    @Test
    void frontFaceProjectionIgnoresDepth() {
        BoundingBox target = new BoundingBox(0, 5, 0, 2, 2, 2);
        BoundingBox inFrontAligned = new BoundingBox(0, 0, 0, 2, 2, 2);   // same X/Z column
        BoundingBox inFrontBeside = new BoundingBox(5, 0, 0, 2, 2, 2);    // different X column

        assertThat(target.overlapsFrontFace(inFrontAligned)).isTrue();
        assertThat(target.overlapsFrontFace(inFrontBeside)).isFalse();
    }

    @Test
    void isInFrontOfComparesDepthOnly() {
        BoundingBox near = new BoundingBox(0, 0, 0, 1, 1, 1);
        BoundingBox far = new BoundingBox(0, 5, 0, 1, 1, 1);
        assertThat(near.isInFrontOf(far)).isTrue();
        assertThat(far.isInFrontOf(near)).isFalse();
    }

    @Test
    void containmentIsCheckedOnEveryAxis() {
        Dimensions container = new Dimensions(10, 10, 10);
        assertThat(new BoundingBox(0, 0, 0, 10, 10, 10).fitsInContainer(container)).isTrue();
        assertThat(new BoundingBox(5, 5, 5, 6, 1, 1).fitsInContainer(container)).isFalse();
        assertThat(new BoundingBox(-1, 0, 0, 1, 1, 1).fitsInContainer(container)).isFalse();
    }
}
