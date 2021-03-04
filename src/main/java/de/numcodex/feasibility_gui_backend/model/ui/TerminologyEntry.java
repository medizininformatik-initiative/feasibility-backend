package de.numcodex.feasibility_gui_backend.model.ui;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.model.common.TermCode;
import de.numcodex.feasibility_gui_backend.model.common.ValueDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class TerminologyEntry {

  @JsonProperty("children")
  private List<TerminologyEntry> children = new ArrayList<>();
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
  private List<ValueDefinition> valueDefinition = new ArrayList<>();
  @JsonProperty("display")
  private String display;

  public static TerminologyEntry copyWithDirectChildren(TerminologyEntry other) {
    var terminology_entry = copyWithoutChildren(other);
    terminology_entry.children = other.children.stream().map(TerminologyEntry::copyWithoutChildren)
        .collect(
            Collectors.toList());
    return terminology_entry;
  }

  public static TerminologyEntry copyWithoutChildren(TerminologyEntry other) {
    var terminologyEntry = new TerminologyEntry();
    terminologyEntry.termCode = other.termCode;
    terminologyEntry.id = other.id;
    terminologyEntry.leaf = other.leaf;
    terminologyEntry.selectable = other.selectable;
    terminologyEntry.timeRestrictionAllowed = other.timeRestrictionAllowed;
    terminologyEntry.valueDefinition = other.valueDefinition;
    terminologyEntry.display = other.display;
    return terminologyEntry;
  }

}
