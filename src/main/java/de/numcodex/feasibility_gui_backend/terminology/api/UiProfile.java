package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UiProfile {
  private String name;
  private boolean timeRestrictionAllowed;
  private String valueDefinition;
  private List<AttributeDefinition> attributeDefinitions;
}
