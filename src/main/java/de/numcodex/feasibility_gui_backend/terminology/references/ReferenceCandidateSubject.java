package de.numcodex.feasibility_gui_backend.terminology.references;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import java.util.List;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class ReferenceCandidateSubject {

  @JsonProperty(value = "termCode", required = true)
  private TermCode termCode;

  @JsonProperty(value = "attributeCodes")
  private List<TermCode> attributeCodes;
}
