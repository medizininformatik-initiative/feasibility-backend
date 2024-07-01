package de.numcodex.feasibility_gui_backend.terminology.es.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TermFilter {
  private String name;
  private String type;
  private List<TermFilterValue> values;
}
