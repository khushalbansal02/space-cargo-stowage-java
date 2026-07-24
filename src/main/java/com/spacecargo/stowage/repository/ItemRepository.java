package com.spacecargo.stowage.repository;

import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.ItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, String> {

    List<Item> findByStatus(ItemStatus status);

    List<Item> findByContainerId(String containerId);

    /** All items currently stowed in a container (used for obstruction/packing checks). */
    List<Item> findByContainerIdAndStatus(String containerId, ItemStatus status);

    Optional<Item> findFirstByNameContainingIgnoreCase(String namePart);
}
