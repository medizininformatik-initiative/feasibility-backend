package de.numcodex.feasibility_gui_backend.terminology.api;

import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.terminology.es.model.CodeableConceptDocument;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CcSearchResultEntry {
  private List<String> valueSets;
  private TermCode termCode;

  public static CcSearchResultEntry of(CodeableConceptDocument codeableConceptDocument) {
    return CcSearchResultEntry.builder()
        .valueSets(codeableConceptDocument.valueSets())
        .termCode(codeableConceptDocument.termCode())
        .build();
  }
}
