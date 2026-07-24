-- Single-row table holding the current simulated station date.
CREATE TABLE simulation_state (
    id       INTEGER PRIMARY KEY,
    sim_date DATE NOT NULL
);

INSERT INTO simulation_state (id, sim_date) VALUES (1, CURRENT_DATE);
