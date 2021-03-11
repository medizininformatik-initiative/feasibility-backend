package de.numcodex.feasibility_gui_backend.model.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.model.common.TermCode;
import de.numcodex.feasibility_gui_backend.model.common.Unit;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ValueFilter {

  @JsonProperty(value = "type", required = true)
  private ValueFilterType type;
  @JsonProperty("selectableConcepts")
  private List<TermCode> selectableConcepts = new ArrayList<>();
  @JsonProperty("comparator")
  private Comparator comparator;
  @JsonProperty("quantityUnit")
  private Unit quantityUnit;
  @JsonProperty(value = "value")
  private Double value;
  @JsonProperty(value = "minValue")
  private Double minValue;
  @JsonProperty(value = "maxValue")
  private Double maxValue;
}
