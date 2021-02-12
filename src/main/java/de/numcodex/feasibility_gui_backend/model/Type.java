package de.numcodex.feasibility_gui_backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Type {
  @JsonProperty("concept")
  CONCEPT,
  @JsonProperty("quantity-comparator")
  QUANTITY_COMPARATOR,
  @JsonProperty("quantity-range")
  QUANTITY_RANGE
}
