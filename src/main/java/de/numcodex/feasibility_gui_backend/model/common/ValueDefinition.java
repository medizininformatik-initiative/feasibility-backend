package de.numcodex.feasibility_gui_backend.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.model.query.Comparator;
import de.numcodex.feasibility_gui_backend.model.ui.ValueType;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ValueDefinition {

  @JsonProperty("type")
  private ValueType type;
  @JsonProperty("selectableConcepts")
  private List<TermCode> selectableConcepts = new ArrayList<>();
  @JsonProperty("comparator")
  private Comparator comparator;
  @JsonProperty("value")
  private double value;
  @JsonProperty("allowedUnits")
  private List<Unit> allowedUnits = new ArrayList<>();
  @JsonProperty("precision")
  private double precision;
  @JsonProperty("min")
  private double min;
  @JsonProperty("max")
  private double max;
}
