package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.Comparator;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record AttributeDefinition(
    @JsonProperty(value = "display", required = true) DisplayEntry display,
    @JsonProperty(value = "type", required = true) ValueDefinitonType type,
    @JsonProperty("selectableConcepts") List<TermCode> selectableConcepts,
    @JsonProperty("attributeCode") TermCode attributeCode,
    @JsonProperty("comparator") Comparator comparator,
    @JsonProperty("optional") Boolean optional,
    @JsonProperty("allowedUnits") List<TermCode> allowedUnits,
    @JsonProperty(value = "precision", required = true, defaultValue = "0") double precision,
    @JsonProperty(value = "min") Double min,
    @JsonProperty(value = "max") Double max,
    @JsonProperty("referencedCriteriaSet") String referencedCriteriaSet,
    @JsonProperty("referencedValueSet") List<String> referencedValueSets
) {
    public AttributeDefinition {
        selectableConcepts = (selectableConcepts == null) ? List.of() : selectableConcepts;
        allowedUnits = (allowedUnits == null) ? List.of() : allowedUnits;
        referencedValueSets = (referencedValueSets == null) ? List.of() : referencedValueSets;
    }
}