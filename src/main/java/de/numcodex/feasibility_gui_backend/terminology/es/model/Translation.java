package de.numcodex.feasibility_gui_backend.terminology.es.model;

import lombok.Builder;

@Builder
public record Translation(
    String lang,
    String value
) {
}
