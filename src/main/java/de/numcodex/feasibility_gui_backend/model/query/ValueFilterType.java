package de.numcodex.feasibility_gui_backend.model.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonInclude(Include.NON_NULL)
public enum ValueFilterType {
  @JsonProperty("concept")
  CONCEPT,
  @JsonProperty("quantity-comparator")
  QUANTITY_COMPARATOR,
  @JsonProperty("quantity-range")
  QUANTITY_RANGE
}
