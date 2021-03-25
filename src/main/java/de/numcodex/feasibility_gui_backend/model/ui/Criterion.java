package de.numcodex.feasibility_gui_backend.model.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.model.common.TermCode;
import de.numcodex.feasibility_gui_backend.model.query.ValueFilter;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class Criterion {

  @JsonProperty("termCode")
  private TermCode termCode;

  @JsonProperty("valueFilter")
  private ValueFilter valueFilter;

}
