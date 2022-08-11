package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import java.util.List;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class Query {

  @JsonProperty
  private long id;
  @JsonProperty
  private StructuredQuery content;
  @JsonProperty
  private String label;
  @JsonProperty
  private String comment;
  @JsonProperty
  private QueryResult results;
  @JsonProperty
  private List<TermCode> invalidTerms;

}
