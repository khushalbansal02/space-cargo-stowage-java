package com.spacecargo.stowage.web.dto;

import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.waste.WasteEntry;

/** A waste item plus the reason it is waste and its current location. */
public record WasteItemView(
        String itemId,
        String name,
        String reason,
        String status,
        Double mass,
        String containerId,
        PlacementView placement) {

    public static WasteItemView from(WasteEntry entry) {
        Item i = entry.item();
        return new WasteItemView(
                i.getItemId(), i.getName(), entry.reason().name(), i.getStatus().name(),
                i.getMass(), i.getContainerId(), PlacementView.from(i));
    }
}
