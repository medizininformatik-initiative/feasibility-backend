package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UiProfile {
  private String name;
  private boolean timeRestrictionAllowed;
  private AttributeDefinition valueDefinition;
  private List<AttributeDefinition> attributeDefinitions;
}
