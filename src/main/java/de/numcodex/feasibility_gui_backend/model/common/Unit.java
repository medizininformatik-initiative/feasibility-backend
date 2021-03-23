package de.numcodex.feasibility_gui_backend.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class Unit {

  @JsonProperty("code")
  private String code;
  @JsonProperty("display")
  private String display;
}
