package de.numcodex.feasibility_gui_backend.terminology.references;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class ReferenceCandidateObject {

  @JsonProperty(value = "uiIndex", required = true)
  private String id;

  @JsonProperty(value = "termCode", required = true)
  private TermCode termCode;
}
