package com.spacecargo.stowage.web;

import com.spacecargo.stowage.service.WasteService;
import com.spacecargo.stowage.web.dto.WasteDtos.ReturnPlanRequest;
import com.spacecargo.stowage.web.dto.WasteDtos.ReturnPlanResponse;
import com.spacecargo.stowage.web.dto.WasteDtos.UndockRequest;
import com.spacecargo.stowage.web.dto.WasteDtos.UndockResponse;
import com.spacecargo.stowage.web.dto.WasteDtos.WasteResponse;
import com.spacecargo.stowage.web.dto.WasteItemView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/waste")
public class WasteController {

    private final WasteService waste;

    public WasteController(WasteService waste) {
        this.waste = waste;
    }

    @GetMapping
    public WasteResponse identify() {
        List<WasteItemView> items = waste.identifyWaste().stream().map(WasteItemView::from).toList();
        return new WasteResponse(items.size(), items);
    }

    @PostMapping("/return-plan")
    public ReturnPlanResponse plan(@Valid @RequestBody ReturnPlanRequest request) {
        return ReturnPlanResponse.from(waste.planReturn(request.undockingContainerId(), request.maxMass()));
    }

    @PostMapping("/undock")
    public UndockResponse undock(@Valid @RequestBody UndockRequest request) {
        return new UndockResponse(waste.completeUndocking(request.itemIds(), request.userIdOrDefault()));
    }
}
