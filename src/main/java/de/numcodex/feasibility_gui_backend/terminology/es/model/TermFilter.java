package de.numcodex.feasibility_gui_backend.terminology.es.model;

import lombok.Builder;

import java.util.List;

@Builder
public record TermFilter(
    String name,
    String type,
    List<TermFilterValue> values
) {
}
