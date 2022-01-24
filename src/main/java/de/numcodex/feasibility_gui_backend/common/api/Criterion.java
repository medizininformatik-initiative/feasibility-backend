package de.numcodex.feasibility_gui_backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.AttributeFilter;
import de.numcodex.feasibility_gui_backend.query.api.TimeRestriction;
import de.numcodex.feasibility_gui_backend.query.api.ValueFilter;
import lombok.Data;

import java.util.ArrayList;

@Data
@JsonInclude(Include.NON_NULL)
public class Criterion {

    @JsonProperty("termCodes")
    private ArrayList<TermCode> termCodes;

    @JsonProperty("attributeFilters")
    private ArrayList<AttributeFilter> attributeFilters;

    @JsonProperty("valueFilter")
    private ValueFilter valueFilter;

    @JsonProperty("timeRestriction")
    private TimeRestriction timeRestriction;
}
