package com.spacecargo.stowage.service;

import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.ItemStatus;
import com.spacecargo.stowage.exception.NotFoundException;
import com.spacecargo.stowage.repository.ItemRepository;
import com.spacecargo.stowage.web.dto.ItemRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ItemService {

    private final ItemRepository items;

    public ItemService(ItemRepository items) {
        this.items = items;
    }

    @Transactional
    public Item create(ItemRequest request) {
        if (items.existsById(request.itemId())) {
            throw new IllegalStateException("Item '" + request.itemId() + "' already exists.");
        }
        Item item = new Item();
        item.setItemId(request.itemId());
        item.setName(request.name());
        item.setDimensionW(request.width());
        item.setDimensionD(request.depth());
        item.setDimensionH(request.height());
        item.setMass(request.mass());
        item.setPriority(request.priority());
        item.setExpiryDate(request.expiryDate());
        item.setUsageLimit(request.usageLimit());
        item.setRemainingUses(request.usageLimit()); // remaining starts at the limit
        item.setPreferredZone(request.preferredZone());
        item.setPreferredContainerId(request.preferredContainerId());
        item.setStatus(ItemStatus.AVAILABLE);
        return items.save(item);
    }

    @Transactional(readOnly = true)
    public List<Item> find(ItemStatus status) {
        return status == null ? items.findAll() : items.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Item getById(String itemId) {
        return items.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item '" + itemId + "' not found."));
    }
}
