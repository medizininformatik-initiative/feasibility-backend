package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record Attribute(
    @JsonProperty String attributeRef,
    @JsonProperty boolean mustHave,
    @JsonProperty List<String> linkedGroups
) {
}
