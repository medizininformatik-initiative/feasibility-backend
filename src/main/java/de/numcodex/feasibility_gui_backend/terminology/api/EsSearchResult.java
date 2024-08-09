package de.numcodex.feasibility_gui_backend.terminology.api;

import lombok.Builder;

import java.util.List;

@Builder
public record EsSearchResult(
    long totalHits,
    List<EsSearchResultEntry> results
) {
}
