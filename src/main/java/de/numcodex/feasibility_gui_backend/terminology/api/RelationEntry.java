package de.numcodex.feasibility_gui_backend.terminology.api;

import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.model.OntologyItemRelationsDocument;
import de.numcodex.feasibility_gui_backend.terminology.es.model.Relative;
import lombok.Builder;

import java.util.Collection;

@Builder
public record RelationEntry(
    DisplayEntry display,
    Collection<Relative> parents,
    Collection<Relative> children,
    Collection<Relative> relatedTerms
) {

  public static RelationEntry of(OntologyItemRelationsDocument document) {
    return RelationEntry.builder()
        .display(DisplayEntry.of(document.display()))
        .parents(document.parents())
        .children(document.children())
        .relatedTerms(document.relatedTerms())
        .build();
  }
}
