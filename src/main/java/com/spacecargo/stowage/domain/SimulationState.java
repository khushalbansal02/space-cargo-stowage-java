package com.spacecargo.stowage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Single-row entity (id = 1) tracking the current simulated station date. */
@Entity
@Table(name = "simulation_state")
public class SimulationState {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    @Column(name = "sim_date", nullable = false)
    private LocalDate simDate;

    protected SimulationState() {
    }

    public Integer getId() { return id; }

    public LocalDate getSimDate() { return simDate; }

    public void setSimDate(LocalDate simDate) { this.simDate = simDate; }
}
