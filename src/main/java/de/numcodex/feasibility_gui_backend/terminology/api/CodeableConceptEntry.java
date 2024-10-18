package de.numcodex.feasibility_gui_backend.terminology.api;

import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import de.numcodex.feasibility_gui_backend.dse.api.LocalizedValue;
import de.numcodex.feasibility_gui_backend.terminology.es.model.CodeableConceptDocument;
import lombok.Builder;

import java.util.List;

@Builder
public record CodeableConceptEntry(
    TermCode termCode,
    DisplayEntry display
) {
  public static CodeableConceptEntry of(CodeableConceptDocument document) {
    return CodeableConceptEntry.builder()
        .termCode(document.termCode())
        .display(DisplayEntry.builder()
            .original(document.display().original())
            .translations(List.of(
                LocalizedValue.builder()
                    .language("de-DE")
                    .value(document.display().deDe())
                    .build(),
                LocalizedValue.builder()
                    .language("en-US")
                    .value(document.display().enUs())
                    .build()
            ))
            .build()
        )
        .build();
  }
}
