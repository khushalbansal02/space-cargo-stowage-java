package com.spacecargo.stowage.web.dto;

import com.spacecargo.stowage.domain.Container;

/** Container view returned to clients. */
public record ContainerResponse(
        String containerId,
        String zone,
        double width,
        double depth,
        double height) {

    public static ContainerResponse from(Container c) {
        return new ContainerResponse(c.getContainerId(), c.getZone(), c.getWidth(), c.getDepth(), c.getHeight());
    }
}
