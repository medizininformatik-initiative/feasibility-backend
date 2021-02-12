package de.numcodex.feasibility_gui_backend.model;

import java.util.List;
import lombok.Data;

@Data
public class TerminologyEntry {

  private List<TerminologyEntry> children;
  private TermCode termCode;

}
