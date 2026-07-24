package com.spacecargo.stowage.web.dto;

import com.spacecargo.stowage.retrieval.RetrievalSearchResult;
import com.spacecargo.stowage.retrieval.RetrievalStep;

import java.util.List;

/** Search result: whether an item was found, its details, and its retrieval plan. */
public record SearchResponse(boolean found, ItemResponse item, List<Step> retrievalSteps) {

    public record Step(int step, String action, String itemId, String itemName) {
        static Step from(RetrievalStep s) {
            return new Step(s.step(), s.action().name(), s.itemId(), s.itemName());
        }
    }

    public static SearchResponse notFound() {
        return new SearchResponse(false, null, List.of());
    }

    public static SearchResponse of(RetrievalSearchResult result) {
        List<Step> steps = result.steps().stream().map(Step::from).toList();
        return new SearchResponse(true, ItemResponse.from(result.item()), steps);
    }
}
