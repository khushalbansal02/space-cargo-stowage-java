package com.spacecargo.stowage.web;

import com.spacecargo.stowage.service.ContainerService;
import com.spacecargo.stowage.web.dto.ContainerRequest;
import com.spacecargo.stowage.web.dto.ContainerResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/containers")
public class ContainerController {

    private final ContainerService containers;

    public ContainerController(ContainerService containers) {
        this.containers = containers;
    }

    @GetMapping
    public List<ContainerResponse> list() {
        return containers.findAll().stream().map(ContainerResponse::from).toList();
    }

    @GetMapping("/{containerId}")
    public ContainerResponse get(@PathVariable String containerId) {
        return ContainerResponse.from(containers.getById(containerId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContainerResponse create(@Valid @RequestBody ContainerRequest request) {
        return ContainerResponse.from(containers.create(request));
    }
}
