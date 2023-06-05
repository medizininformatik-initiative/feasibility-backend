package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.Comparator;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.Unit;
import java.util.Objects;

import java.util.List;

@JsonInclude(Include.NON_NULL)
public record ValueFilter(
    @JsonProperty(value = "type", required = true) ValueFilterType type,
    @JsonProperty("selectedConcepts") List<TermCode> selectedConcepts,
    @JsonProperty("comparator") Comparator comparator,
    @JsonProperty("unit") Unit quantityUnit,
    @JsonProperty(value = "value") Double value,
    @JsonProperty(value = "minValue") Double minValue,
    @JsonProperty(value = "maxValue")Double maxValue
) {

    public ValueFilter {
        Objects.requireNonNull(type);
    }
}
