package com.spacecargo.stowage.service;

import com.spacecargo.stowage.domain.Container;
import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.ItemStatus;
import com.spacecargo.stowage.domain.SimulationState;
import com.spacecargo.stowage.domain.geometry.BoundingBox;
import com.spacecargo.stowage.repository.ContainerRepository;
import com.spacecargo.stowage.repository.ItemRepository;
import com.spacecargo.stowage.repository.SimulationStateRepository;
import com.spacecargo.stowage.simulate.SimulationRequest;
import com.spacecargo.stowage.simulate.SimulationRequest.ItemUsage;
import com.spacecargo.stowage.simulate.SimulationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SimulationServiceTest {

    private static final LocalDate BASE = LocalDate.of(2026, 1, 1);

    @Autowired SimulationService simulationService;
    @Autowired SimulationStateRepository state;
    @Autowired ContainerRepository containers;
    @Autowired ItemRepository items;

    @BeforeEach
    void reset() {
        items.deleteAll();
        containers.deleteAll();
        SimulationState sim = state.findById(SimulationState.SINGLETON_ID).orElseThrow();
        sim.setSimDate(BASE);
        state.save(sim);
        containers.save(new Container("C1", "Lab", 10, 10, 10));
    }

    private Item stowed(String id) {
        Item item = new Item();
        item.setItemId(id);
        item.setName(id);
        item.setDimensionW(2);
        item.setDimensionD(2);
        item.setDimensionH(2);
        item.setPriority(50);
        item.stow("C1", new BoundingBox(0, 0, 0, 2, 2, 2));
        return item;
    }

    @Test
    void advancingDaysMovesTheSimulationDateForward() {
        SimulationResult result = simulationService.simulate(
                new SimulationRequest(5, null, List.of()), "tester");

        assertThat(result.newDate()).isEqualTo(BASE.plusDays(5));
        assertThat(simulationService.currentDate()).isEqualTo(BASE.plusDays(5));
    }

    @Test
    void itemsPastExpiryBecomeExpired() {
        Item item = stowed("PERISHABLE");
        item.setExpiryDate(BASE.plusDays(2));
        items.save(item);

        SimulationResult result = simulationService.simulate(
                new SimulationRequest(3, null, List.of()), "tester");

        assertThat(result.expired()).extracting("itemId").containsExactly("PERISHABLE");
        assertThat(items.findById("PERISHABLE").orElseThrow().getStatus()).isEqualTo(ItemStatus.EXPIRED);
    }

    @Test
    void dailyUsageDepletesAndConsumesAnItem() {
        Item item = stowed("BATTERY");
        item.setUsageLimit(3);
        item.setRemainingUses(3);
        items.save(item);

        SimulationResult result = simulationService.simulate(
                new SimulationRequest(3, null, List.of(new ItemUsage("BATTERY", 1))), "tester");

        assertThat(result.consumed()).extracting("itemId").containsExactly("BATTERY");
        Item after = items.findById("BATTERY").orElseThrow();
        assertThat(after.getRemainingUses()).isZero();
        assertThat(after.getStatus()).isEqualTo(ItemStatus.CONSUMED);
    }

    @Test
    void simulatingToAnEarlierDateIsRejected() {
        assertThatThrownBy(() -> simulationService.simulate(
                new SimulationRequest(null, BASE.minusDays(1), List.of()), "tester"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiringExactlyOneOfDaysOrTargetIsEnforced() {
        assertThatThrownBy(() -> simulationService.simulate(
                new SimulationRequest(5, BASE.plusDays(5), List.of()), "tester"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
