package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;

@Setter
@JsonInclude(Include.NON_NULL)
public class TimeRestriction {
    @JsonProperty("beforeDate")
    private String beforeDate;

    @JsonProperty("afterDate")
    private String afterDate;
}
