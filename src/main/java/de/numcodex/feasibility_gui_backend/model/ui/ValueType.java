package de.numcodex.feasibility_gui_backend.model.ui;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ValueType {
  @JsonProperty("concept")
  CONCEPT,
  @JsonProperty("quantity")
  QUANTITY
}
