package com.spacecargo.stowage.web.dto;

import com.spacecargo.stowage.waste.WasteReturnPlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** Request/response payloads for the waste-management endpoints. */
public final class WasteDtos {

    private WasteDtos() {
    }

    public record WasteResponse(int count, List<WasteItemView> items) {
    }

    public record ReturnPlanRequest(
            @NotBlank String undockingContainerId,
            @Positive Double maxMass) {
    }

    public record ReturnPlanResponse(
            String undockingContainerId,
            int itemCount,
            double totalMass,
            double totalVolume,
            List<WasteItemView> items) {

        public static ReturnPlanResponse from(WasteReturnPlan plan) {
            List<WasteItemView> views = plan.entries().stream().map(WasteItemView::from).toList();
            return new ReturnPlanResponse(plan.undockingContainerId(), plan.itemCount(),
                    plan.totalMass(), plan.totalVolume(), views);
        }
    }

    public record UndockRequest(
            @NotEmpty List<String> itemIds,
            String userId) {

        public String userIdOrDefault() {
            return (userId == null || userId.isBlank()) ? "system" : userId;
        }
    }

    public record UndockResponse(int disposed) {
    }
}
