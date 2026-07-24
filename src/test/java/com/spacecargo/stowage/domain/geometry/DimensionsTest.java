package com.spacecargo.stowage.domain.geometry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DimensionsTest {

    @Test
    void rejectsNonPositiveSides() {
        assertThatThrownBy(() -> new Dimensions(0, 1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Dimensions(1, -1, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void computesVolume() {
        assertThat(new Dimensions(2, 3, 4).volume()).isEqualTo(24.0);
    }

    @Test
    void aCuboidWithThreeDistinctSidesHasSixOrientations() {
        List<Dimensions> orientations = new Dimensions(1, 2, 3).orientations();
        assertThat(orientations).hasSize(6);
    }

    @Test
    void aCubeCollapsesToASingleOrientation() {
        assertThat(new Dimensions(5, 5, 5).orientations()).hasSize(1);
    }

    @Test
    void twoEqualSidesGiveThreeOrientations() {
        assertThat(new Dimensions(2, 2, 5).orientations()).hasSize(3);
    }

    @Test
    void fitsWithinRespectsEachAxis() {
        Dimensions container = new Dimensions(10, 10, 10);
        assertThat(new Dimensions(10, 10, 10).fitsWithin(container)).isTrue();
        assertThat(new Dimensions(11, 1, 1).fitsWithin(container)).isFalse();
    }
}
