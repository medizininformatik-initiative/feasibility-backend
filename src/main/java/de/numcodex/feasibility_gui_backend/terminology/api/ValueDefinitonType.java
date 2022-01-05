package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public enum ValueDefinitonType {
    @JsonProperty("concept")
    CONCEPT,
    @JsonProperty("quantity")
    QUANTITY
}
