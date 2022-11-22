package de.numcodex.feasibility_gui_backend.terminology.references;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class ReferenceCandidateRequest {

  @JsonProperty(value = "parent", required = true)
  private ReferenceCandidateSubject parent;

  @JsonProperty(value = "selectedTerms", required = true)
  private List<ReferenceCandidateObject> referenceCandidates;
}
