package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UiProfile {

  @JsonProperty(value = "name")
  private String name;

  @JsonProperty(value = "timeRestrictionAllowed")
  private boolean timeRestrictionAllowed;

  @JsonProperty(value = "valueDefinition")
  private AttributeDefinition valueDefinition;

  @JsonProperty(value = "attributeDefinitions")
  private List<AttributeDefinition> attributeDefinitions;
}