package de.numcodex.feasibility_gui_backend.terminology.api;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CcSearchResult {
  private long totalHits;
  private List<CcSearchResultEntry> results;
}
