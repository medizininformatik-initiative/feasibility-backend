package de.numcodex.feasibility_gui_backend.terminology.es.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Translation {

  private String lang;
  private String value;
}
