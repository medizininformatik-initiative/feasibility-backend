package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.Comparator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(Include.NON_NULL)
public class AttributeDefinition {

    @JsonProperty(value = "type", required = true)
    private ValueDefinitonType type;
    @JsonProperty("selectableConcepts")
    private List<TermCode> selectableConcepts = new ArrayList<>();
    @JsonProperty("attributeCode")
    private TermCode attributeCode;
    @JsonProperty("comparator")
    private Comparator comparator;
    @JsonProperty("optional")
    private Boolean optional;
    @JsonProperty("allowedUnits")
    private List<TermCode> allowedUnits = new ArrayList<>();
    @JsonProperty(value = "precision", required = true, defaultValue = "0")
    private double precision;
    @JsonProperty(value = "min")
    private Double min;
    @JsonProperty(value = "max")
    private Double max;
    @JsonProperty("referencedCriteriaSet")
    private String referencedCriteriaSet;
    @JsonProperty("referencedValueSet")
    private String referencedValueSet;
}
