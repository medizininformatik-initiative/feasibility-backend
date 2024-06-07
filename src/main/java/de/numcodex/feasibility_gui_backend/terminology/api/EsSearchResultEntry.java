package de.numcodex.feasibility_gui_backend.terminology.api;

import de.numcodex.feasibility_gui_backend.terminology.es.model.OntologyListItemDocument;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EsSearchResultEntry {
  private String id;
  private String name;
  private int availability;
  private String context;
  private String terminology;
  private String termcode;
  private String kdsModule;
  private boolean selectable;

  public static EsSearchResultEntry of(OntologyListItemDocument ontologyListItemDocument) {
    return EsSearchResultEntry.builder()
        .id(ontologyListItemDocument.getId())
        .name(ontologyListItemDocument.getName())
        .availability(ontologyListItemDocument.getAvailability())
        .context(ontologyListItemDocument.getContext().code())
        .terminology(ontologyListItemDocument.getTerminology())
        .termcode(ontologyListItemDocument.getTermcode())
        .kdsModule(ontologyListItemDocument.getKdsModule())
        .selectable(ontologyListItemDocument.isSelectable())
        .build();
  }
}
