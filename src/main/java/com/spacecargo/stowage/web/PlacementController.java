package com.spacecargo.stowage.web;

import com.spacecargo.stowage.service.PlacementService;
import com.spacecargo.stowage.web.dto.PlacementRequest;
import com.spacecargo.stowage.web.dto.PlacementResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/placements")
public class PlacementController {

    private final PlacementService placement;

    public PlacementController(PlacementService placement) {
        this.placement = placement;
    }

    @PostMapping
    public PlacementResponse place(@RequestBody(required = false) PlacementRequest request) {
        PlacementRequest req = request == null ? new PlacementRequest(null, null) : request;
        return PlacementResponse.from(placement.placeItems(req.itemIds(), req.userIdOrDefault()));
    }
}
