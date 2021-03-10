package de.numcodex.feasibility_gui_backend.model.ui;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ValueDefinitonType {
  @JsonProperty("concept")
  CONCEPT,
  @JsonProperty("quantity")
  QUANTITY
}
