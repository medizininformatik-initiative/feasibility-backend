package de.numcodex.feasibility_gui_backend.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class TermCode {

  @JsonProperty("code")
  private String code;
  @JsonProperty("system")
  private String system;
  @JsonProperty("version")
  private String version;
  @JsonProperty("display")
  private String display;

}
