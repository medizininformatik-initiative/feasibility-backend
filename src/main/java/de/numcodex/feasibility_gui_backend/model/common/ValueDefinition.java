package de.numcodex.feasibility_gui_backend.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.model.query.Comparator;
import de.numcodex.feasibility_gui_backend.model.query.Type;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ValueDefinition {

  @JsonProperty("type")
  private Type type;
  @JsonProperty("termCode")
  private List<TermCode> termCode = new ArrayList<>();
  @JsonProperty("comparator")
  private Comparator comparator;
  @JsonProperty("value")
  private double value;
  @JsonProperty("unit")
  private Unit unit;
  @JsonProperty("minValue")
  private double minValue;
  @JsonProperty("maxValue")
  private double maxValue;
}
