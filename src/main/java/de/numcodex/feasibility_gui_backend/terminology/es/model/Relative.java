package de.numcodex.feasibility_gui_backend.terminology.es.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Relative {

  private String name;
  private String contextualizedTermcodeHash;
}
