package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import lombok.Data;
import org.springframework.data.util.Pair;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CriteriaSet {
  private String url;
  @JsonProperty(value = "contextualized_term_codes")
  private List<Pair<TermCode, TermCode>> contextualizedTermCodes;
}
