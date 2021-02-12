package de.numcodex.feasibility_gui_backend.model;

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

  public Optional<String> getVersion() {
    return Optional.ofNullable(version);
  }

  public Optional<String> getDisplay() {
    return Optional.ofNullable(display);
  }
}
