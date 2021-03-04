package de.numcodex.feasibility_gui_backend.model.ui;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.model.common.TermCode;
import de.numcodex.feasibility_gui_backend.model.common.ValueDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class TerminologyEntry {
  @JsonProperty("children")
  private List<TerminologyEntry> children;
  @JsonProperty("termCode")
  private TermCode termCode;
  @JsonProperty("id")
  private UUID id;
  @JsonProperty("leaf")
  private boolean leaf;
  @JsonProperty("selectable")
  private boolean selectable;
  @JsonProperty("timeRestrictionAllowed")
  private boolean timeRestrictionAllowed;
  @JsonProperty("valueDefinition")
  private ValueDefinition valueDefinition;
  @JsonProperty("display")
  private String display;

  public void copy(TerminologyEntry other)
  {
    children = new ArrayList<>();
    // TODO: Works but can you do that more elegantly?
    for(var child : other.getChildren())
    {
      var child_without_children = new TerminologyEntry();
      child_without_children.copy(child);
      child_without_children.getChildren().clear();
      children.add(child_without_children);
    }
    termCode = other.termCode;
    id = other.id;
    leaf = other.leaf;
    selectable = other.selectable;
    timeRestrictionAllowed = other.timeRestrictionAllowed;
    valueDefinition = other.valueDefinition;
    display = other.display;
  }
}
