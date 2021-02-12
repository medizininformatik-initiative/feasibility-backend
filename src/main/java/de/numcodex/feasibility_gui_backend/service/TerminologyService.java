package de.numcodex.feasibility_gui_backend.service;

import de.numcodex.feasibility_gui_backend.model.CategoryEntry;
import de.numcodex.feasibility_gui_backend.model.TerminologyEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TerminologyService {

  public TerminologyEntry getEntry(UUID nodeId) {
    return null;
  }

  public List<CategoryEntry> getCategories() {
    //Client.request("endpoint") -> List<CategoryEntry>
    return null;
  }

  public List<TerminologyEntry> queryEntries(String query) {
    return null;
  }

  public List<TerminologyEntry> getValueSet(String ValueSet) {
    return null;
  }
}
