package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import java.util.List;
import javax.persistence.Column;
import lombok.Data;

@Data
@JsonInclude(Include.NON_EMPTY)
public class StoredQuery {

  @JsonProperty
  private long id;
  @JsonProperty
  private StructuredQuery structuredQuery;
  @JsonProperty
  private String label;
  @JsonProperty
  private String comment;
  @JsonProperty
  private String lastModified;
  @JsonProperty
  private String createdBy;
  @JsonProperty
  private List<TermCode> outdatedTerms;

}
