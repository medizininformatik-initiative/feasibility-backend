package de.numcodex.feasibility_gui_backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import lombok.Data;

@Data
public class Criterion {

  @JsonProperty("termCode")
  private TermCode termCode;
  @JsonProperty("valueFilter")
  private ValueFilter valueFilter;

  public Optional<ValueFilter> getValueFilter() {
    return Optional.ofNullable(valueFilter);
  }

  public TermCode getTermCode() {
    return termCode;
  }

  public void setValueFilter(ValueFilter valueFilter) {
    this.valueFilter = valueFilter;
  }
}
