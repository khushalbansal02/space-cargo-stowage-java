package com.spacecargo.stowage.web.dto;

import com.spacecargo.stowage.placement.PlacementOutcome;

import java.util.List;

/** Result of a placement run: per-item outcomes plus summary counts. */
public record PlacementResponse(int requested, int placed, int failed, List<Outcome> results) {

    public record Outcome(
            String itemId,
            boolean placed,
            String containerId,
            PlacementView placement,
            boolean preferred,
            String reason) {

        static Outcome from(PlacementOutcome o) {
            PlacementView view = o.placed() ? PlacementView.from(o.containerId(), o.box()) : null;
            return new Outcome(o.itemId(), o.placed(), o.containerId(), view, o.preferred(), o.reason());
        }
    }

    public static PlacementResponse from(List<PlacementOutcome> outcomes) {
        List<Outcome> results = outcomes.stream().map(Outcome::from).toList();
        int placed = (int) outcomes.stream().filter(PlacementOutcome::placed).count();
        return new PlacementResponse(outcomes.size(), placed, outcomes.size() - placed, results);
    }
}
