package de.numcodex.feasibility_gui_backend.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import lombok.Data;

@Data
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
