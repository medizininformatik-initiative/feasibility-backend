package de.numcodex.feasibility_gui_backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public record Unit(
    @JsonProperty("code") String code,
    @JsonProperty("display") String display
) {

}
