package com.spacecargo.stowage.web;

import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.service.RetrievalService;
import com.spacecargo.stowage.web.dto.ItemResponse;
import com.spacecargo.stowage.web.dto.RetrieveRequest;
import com.spacecargo.stowage.web.dto.SearchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Locating and retrieving items. Search lives at {@code /api/v1/search}; confirming
 * a retrieval is a sub-action of an item resource.
 */
@RestController
public class RetrievalController {

    private final RetrievalService retrieval;

    public RetrievalController(RetrievalService retrieval) {
        this.retrieval = retrieval;
    }

    @GetMapping("/api/v1/search")
    public SearchResponse search(@RequestParam(required = false) String itemId,
                                 @RequestParam(required = false) String itemName) {
        return retrieval.search(itemId, itemName)
                .map(SearchResponse::of)
                .orElseGet(SearchResponse::notFound);
    }

    @PostMapping("/api/v1/items/{itemId}/retrieve")
    public ItemResponse retrieve(@PathVariable String itemId,
                                 @RequestBody(required = false) RetrieveRequest request) {
        RetrieveRequest req = request == null ? new RetrieveRequest(null) : request;
        Item updated = retrieval.confirmRetrieval(itemId, req.userIdOrDefault());
        return ItemResponse.from(updated);
    }
}
