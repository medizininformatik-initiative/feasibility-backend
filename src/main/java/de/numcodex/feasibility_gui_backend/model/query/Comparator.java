package de.numcodex.feasibility_gui_backend.model.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonInclude(Include.NON_NULL)
public enum Comparator {
  @JsonProperty("eq")
  EQUAL,
  @JsonProperty("ue")
  UNEQUAL,
  @JsonProperty("le")
  LESS_EQUAL,
  @JsonProperty("lt")
  LESS_THAN,
  @JsonProperty("ge")
  GREATER_EQUAL,
  @JsonProperty("gt")
  GREATER_THAN
}
