package de.numcodex.feasibility_gui_backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonInclude(Include.NON_NULL)
@EqualsAndHashCode
public class TermCode {

  @JsonProperty("code")
  private String code;
  @JsonProperty("system")
  private String system;
  @EqualsAndHashCode.Exclude
  @JsonProperty("version")
  private String version;
  @JsonProperty("display")
  @EqualsAndHashCode.Exclude
  private String display;

}
