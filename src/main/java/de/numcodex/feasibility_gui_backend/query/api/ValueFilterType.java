package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.ALWAYS)
public enum ValueFilterType {
    @JsonProperty("concept")
    CONCEPT,
    @JsonProperty("quantity-comparator")
    QUANTITY_COMPARATOR,
    @JsonProperty("quantity-range")
    QUANTITY_RANGE,
    @JsonProperty("reference")
    REFERENCE
}
