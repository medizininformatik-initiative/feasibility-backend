package de.numcodex.feasibility_gui_backend.model.ui;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.model.common.TermCode;
import de.numcodex.feasibility_gui_backend.model.common.Unit;
import de.numcodex.feasibility_gui_backend.model.query.Comparator;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ValueDefinition {

  @JsonProperty(value = "type", required = true)
  private ValueDefinitonType type;
  @JsonProperty("selectableConcepts")
  private List<TermCode> selectableConcepts = new ArrayList<>();
  @JsonProperty("comparator")
  private Comparator comparator;
  @JsonProperty("allowedUnits")
  private List<Unit> allowedUnits = new ArrayList<>();
  @JsonProperty(value = "precision", required = true, defaultValue = "0")
  private double precision;
  @JsonProperty(value = "min")
  private Double min;
  @JsonProperty(value = "max")
  private Double max;
}
