package de.numcodex.feasibility_gui_backend.terminology.api;

import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CcSearchResult {
  private long totalHits;
  private List<TermCode> results;
}
