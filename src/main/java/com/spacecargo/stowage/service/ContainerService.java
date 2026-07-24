package com.spacecargo.stowage.service;

import com.spacecargo.stowage.domain.Container;
import com.spacecargo.stowage.exception.NotFoundException;
import com.spacecargo.stowage.repository.ContainerRepository;
import com.spacecargo.stowage.web.dto.ContainerRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContainerService {

    private final ContainerRepository containers;

    public ContainerService(ContainerRepository containers) {
        this.containers = containers;
    }

    @Transactional
    public Container create(ContainerRequest request) {
        if (containers.existsById(request.containerId())) {
            throw new IllegalStateException("Container '" + request.containerId() + "' already exists.");
        }
        return containers.save(new Container(
                request.containerId(), request.zone(),
                request.width(), request.depth(), request.height()));
    }

    @Transactional(readOnly = true)
    public List<Container> findAll() {
        return containers.findAll();
    }

    @Transactional(readOnly = true)
    public Container getById(String containerId) {
        return containers.findById(containerId)
                .orElseThrow(() -> new NotFoundException("Container '" + containerId + "' not found."));
    }
}
