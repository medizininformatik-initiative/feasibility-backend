package de.numcodex.feasibility_gui_backend.api;


import de.numcodex.feasibility_gui_backend.model.ui.CategoryEntry;
import de.numcodex.feasibility_gui_backend.model.ui.TerminologyEntry;
import de.numcodex.feasibility_gui_backend.service.TerminologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/*
 Rest interface to get the terminology definitions from the UI backend which itself request the
 terminology information from the ui terminology service
 */


@RequestMapping("api/v1/terminology")
@RestController
@CrossOrigin
public class TerminologyRestController {

  private final TerminologyService terminologyService;

  @Autowired
  public TerminologyRestController(TerminologyService terminologyService) {
    this.terminologyService = terminologyService;
  }

  @GetMapping("entries/{nodeId}")
  public TerminologyEntry getEntry(@PathVariable UUID nodeId) {
    return terminologyService.getEntry(nodeId);
  }

  @GetMapping("root-entries")
  public List<CategoryEntry> getCategories() {
    return terminologyService.getCategories();
  }

  @GetMapping("selectable-entries")
  public List<TerminologyEntry> getSelectableEntries(@RequestParam("query") String query,
      @RequestParam(value = "categoryId", required = false) UUID categoryId) {
    return terminologyService.getSelectableEntries(query, categoryId);
  }
}
