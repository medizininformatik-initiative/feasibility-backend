package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.Comparator;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.Unit;
import java.util.List;
import java.util.Objects;


@JsonInclude(Include.NON_NULL)
public record AttributeFilter(
    @JsonProperty(value = "type", required = true) ValueFilterType type,
    @JsonProperty("selectedConcepts") List<TermCode> selectedConcepts,
    @JsonProperty("comparator") Comparator comparator,
    @JsonProperty("unit") Unit quantityUnit,
    @JsonProperty(value = "value") Double value,
    @JsonProperty(value = "minValue") Double minValue,
    @JsonProperty(value = "maxValue") Double maxValue,
    @JsonProperty(value = "attributeCode", required = true) TermCode attributeCode
) {

    public AttributeFilter {
        Objects.requireNonNull(type);
        Objects.requireNonNull(attributeCode);
    }
}
