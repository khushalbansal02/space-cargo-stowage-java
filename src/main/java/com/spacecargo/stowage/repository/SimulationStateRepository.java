package com.spacecargo.stowage.repository;

import com.spacecargo.stowage.domain.SimulationState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationStateRepository extends JpaRepository<SimulationState, Integer> {
}
