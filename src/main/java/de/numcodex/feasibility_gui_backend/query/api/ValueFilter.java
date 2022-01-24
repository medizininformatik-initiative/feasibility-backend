package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.Comparator;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.Unit;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(Include.NON_NULL)
public class ValueFilter {

    @JsonProperty(value = "type", required = true)
    private ValueFilterType type;
    @JsonProperty("selectedConcepts")
    private List<TermCode> selectedConcepts;
    @JsonProperty("comparator")
    private Comparator comparator;
    @JsonProperty("unit")
    private Unit quantityUnit;
    @JsonProperty(value = "value")
    private Double value;
    @JsonProperty(value = "minValue")
    private Double minValue;
    @JsonProperty(value = "maxValue")
    private Double maxValue;
}
