package de.numcodex.feasibility_gui_backend.terminology.v3;


import de.numcodex.feasibility_gui_backend.terminology.TerminologyService;
import de.numcodex.feasibility_gui_backend.terminology.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.GONE;

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

    @GetMapping(value = "systems", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TerminologySystemEntry> getTerminologySystems() {
        return terminologyService.getTerminologySystems();
    }

    @GetMapping("entries/{nodeId}")
    @ResponseStatus(GONE)
    public void getEntry(@PathVariable UUID nodeId) {
    }

    @GetMapping("categories")
    @ResponseStatus(GONE)
    public void getCategories() {
    }

    @GetMapping("entries")
    @ResponseStatus(GONE)
    public void searchSelectableEntries(@RequestParam("query") String query,
                                                          @RequestParam(value = "categoryId", required = false) UUID categoryId) {
    }

    @GetMapping(value = "{contextualizedTermcodeId}/ui_profile", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(GONE)
    public void getUiProfile(
            @PathVariable("contextualizedTermcodeId") String contextualizedTermcodeId) {
    }

    @GetMapping(value = "{contextualizedTermcodeId}/mapping", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(GONE)
    public void getMapping(
            @PathVariable("contextualizedTermcodeId") String contextualizedTermcodeId) {
    }

    @PostMapping(value = "criteria-set/intersect", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(GONE)
    public void getIntersection(
            @RequestParam("criteriaSetUrl") String criteriaSetUrl,
            @RequestBody List<String> contextTermCodeHashList) {
    }
}
