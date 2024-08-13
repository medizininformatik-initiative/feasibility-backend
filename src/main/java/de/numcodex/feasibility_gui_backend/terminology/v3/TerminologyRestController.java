package de.numcodex.feasibility_gui_backend.terminology.v3;


import de.numcodex.feasibility_gui_backend.terminology.TerminologyService;
import de.numcodex.feasibility_gui_backend.terminology.api.CategoryEntry;
import de.numcodex.feasibility_gui_backend.terminology.api.CriteriaProfileData;
import de.numcodex.feasibility_gui_backend.terminology.api.TerminologyEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/*
 Rest interface to get the terminology definitions from the UI backend which itself request the
 terminology information from the ui terminology service
 */


@RequestMapping("api/v3/terminology")
@RestController
@CrossOrigin
public class TerminologyRestController {

    private final TerminologyService terminologyService;

    @Autowired
    public TerminologyRestController(TerminologyService terminologyService) {
        this.terminologyService = terminologyService;
    }

    @GetMapping("criteria-profile-data")
    public List<CriteriaProfileData> getCriteriaProfileData(@RequestParam List<String> ids) {
        return terminologyService.getCriteriaProfileData(ids);
    }

    @GetMapping("entries/{nodeId}")
    public TerminologyEntry getEntry(@PathVariable UUID nodeId) {
        return terminologyService.getEntry(nodeId);
    }

    @GetMapping("categories")
    public List<CategoryEntry> getCategories() {
        return terminologyService.getCategories();
    }

    @GetMapping("entries")
    public List<TerminologyEntry> searchSelectableEntries(@RequestParam("query") String query,
                                                          @RequestParam(value = "categoryId", required = false) UUID categoryId) {
        return terminologyService.getSelectableEntries(query, categoryId);
    }

    @GetMapping(value = "{contextualizedTermcodeId}/ui_profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getUiProfile(
            @PathVariable("contextualizedTermcodeId") String contextualizedTermcodeId) {
        return terminologyService.getUiProfile(contextualizedTermcodeId);
    }

    @GetMapping(value = "{contextualizedTermcodeId}/mapping", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getMapping(
            @PathVariable("contextualizedTermcodeId") String contextualizedTermcodeId) {
        return terminologyService.getMapping(contextualizedTermcodeId);
    }

    @PostMapping(value = "criteria-set/intersect", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getIntersection(
            @RequestParam("criteriaSetUrl") String criteriaSetUrl,
            @RequestBody List<String> contextTermCodeHashList) {
        return terminologyService.getIntersection(criteriaSetUrl, contextTermCodeHashList);
    }
}
