package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Attribute(
    @JsonProperty String attributeRef,
    @JsonProperty boolean mustHave,
    @JsonProperty List<String> linkedGroups
) {
}
