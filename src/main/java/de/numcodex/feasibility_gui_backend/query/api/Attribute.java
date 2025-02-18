package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Attribute(
    @JsonProperty String attributeRef,
    @JsonProperty boolean mustHave,
    @JsonProperty List<String> linkedGroups
) {
  public Attribute {
    linkedGroups = linkedGroups == null ? List.of() : linkedGroups;
  }
}
