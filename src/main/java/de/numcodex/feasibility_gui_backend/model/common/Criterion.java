package de.numcodex.feasibility_gui_backend.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import lombok.Data;

@Data
public class Criterion {

  @JsonProperty("termCode")
  private TermCode termCode;
  @JsonProperty("valueFilter")
  private ValueDefinition valueDefinition;

  public Optional<ValueDefinition> getValueDefinition() {
    return Optional.ofNullable(valueDefinition);
  }

  public TermCode getTermCode() {
    return termCode;
  }

  public void setValueDefinition(ValueDefinition valueDefinition) {
    this.valueDefinition = valueDefinition;
  }
}
