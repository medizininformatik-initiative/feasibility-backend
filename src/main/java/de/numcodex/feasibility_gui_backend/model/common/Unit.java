package de.numcodex.feasibility_gui_backend.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Unit {

  @JsonProperty("code")
  private String code;
  @JsonProperty("display")
  private String display;
}
