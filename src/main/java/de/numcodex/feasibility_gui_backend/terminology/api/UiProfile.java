package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record UiProfile(
    @JsonProperty("name") String name,
    @JsonProperty("timeRestrictionAllowed") boolean timeRestrictionAllowed,
    @JsonProperty("valueDefinition") AttributeDefinition valueDefinition,
    @JsonProperty("attributeDefinitions") List<AttributeDefinition> attributeDefinitions
) {
  public UiProfile {
    attributeDefinitions = (attributeDefinitions == null) ? List.of() : attributeDefinitions;
  }
}