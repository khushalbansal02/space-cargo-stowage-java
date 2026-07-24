package com.spacecargo.stowage.web;

import com.spacecargo.stowage.domain.ItemStatus;
import com.spacecargo.stowage.service.ItemService;
import com.spacecargo.stowage.web.dto.ItemRequest;
import com.spacecargo.stowage.web.dto.ItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemService items;

    public ItemController(ItemService items) {
        this.items = items;
    }

    @GetMapping
    public List<ItemResponse> list(@RequestParam(required = false) ItemStatus status) {
        return items.find(status).stream().map(ItemResponse::from).toList();
    }

    @GetMapping("/{itemId}")
    public ItemResponse get(@PathVariable String itemId) {
        return ItemResponse.from(items.getById(itemId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse create(@Valid @RequestBody ItemRequest request) {
        return ItemResponse.from(items.create(request));
    }
}
