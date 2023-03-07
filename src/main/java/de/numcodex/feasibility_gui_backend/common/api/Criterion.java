package de.numcodex.feasibility_gui_backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.query.api.AttributeFilter;
import de.numcodex.feasibility_gui_backend.query.api.TimeRestriction;
import de.numcodex.feasibility_gui_backend.query.api.ValueFilter;

import java.util.List;

@JsonInclude(Include.NON_NULL)
public record Criterion (
    @JsonProperty("termCodes") List<TermCode> termCodes,
    @JsonProperty("attributeFilters") List<AttributeFilter> attributeFilters,
    @JsonProperty("valueFilter") ValueFilter valueFilter,
    @JsonProperty("timeRestriction") TimeRestriction timeRestriction
) {

}
