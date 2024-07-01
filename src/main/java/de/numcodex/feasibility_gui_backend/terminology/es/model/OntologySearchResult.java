package de.numcodex.feasibility_gui_backend.terminology.es.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OntologySearchResult {
  private long totalHits;
  private List<OntologyListItemDocument> results;
}
