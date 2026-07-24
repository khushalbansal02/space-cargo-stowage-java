package com.spacecargo.stowage.web;

import com.spacecargo.stowage.service.SimulationService;
import com.spacecargo.stowage.simulate.SimulationRequest;
import com.spacecargo.stowage.simulate.SimulationResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/simulate")
public class SimulationController {

    private final SimulationService simulation;

    public SimulationController(SimulationService simulation) {
        this.simulation = simulation;
    }

    @GetMapping("/current-date")
    public Map<String, LocalDate> currentDate() {
        return Map.of("currentDate", simulation.currentDate());
    }

    @PostMapping("/day")
    public SimulationResult simulate(@RequestBody SimulationRequest request,
                                     @RequestParam(defaultValue = "system") String userId) {
        return simulation.simulate(request, userId);
    }
}
