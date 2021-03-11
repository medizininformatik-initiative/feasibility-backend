package de.numcodex.feasibility_gui_backend.model.query;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ValueFilterType {
  @JsonProperty("concept")
  CONCEPT,
  @JsonProperty("quantity-comparator")
  QUANTITY_COMPARATOR,
  @JsonProperty("quantity-range")
  QUANTITY_RANGE
}
