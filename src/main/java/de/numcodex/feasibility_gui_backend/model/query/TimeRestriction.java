package de.numcodex.feasibility_gui_backend.model.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class TimeRestriction {
  @JsonProperty("beforeDate")
  private String beforeDate;

  @JsonProperty("afterDate")
  private String afterDate;

}
