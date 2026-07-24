package com.spacecargo.stowage.repository;

import com.spacecargo.stowage.domain.Container;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContainerRepository extends JpaRepository<Container, String> {

    List<Container> findByZone(String zone);
}
