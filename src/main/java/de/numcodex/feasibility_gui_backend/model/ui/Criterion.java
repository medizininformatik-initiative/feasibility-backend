package de.numcodex.feasibility_gui_backend.model.ui;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.model.common.TermCode;
import de.numcodex.feasibility_gui_backend.model.query.ValueFilter;
import lombok.Data;

import java.util.Optional;

@Data
public class Criterion {

  @JsonProperty("termCode")
  private TermCode termCode;

  @JsonProperty("valueFilter")
  private ValueFilter valueFilter;


  public Optional<ValueFilter> getValueFilter() {
    return Optional.ofNullable(valueFilter);
  }
}
