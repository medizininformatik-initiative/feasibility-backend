package de.numcodex.feasibility_gui_backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

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
