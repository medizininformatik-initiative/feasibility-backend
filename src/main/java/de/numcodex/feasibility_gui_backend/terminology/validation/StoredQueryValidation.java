package de.numcodex.feasibility_gui_backend.terminology.validation;

import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.StoredQuery;
import de.numcodex.feasibility_gui_backend.terminology.TerminologyService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StoredQueryValidation {

  private final TerminologyService terminologyService;

  @Autowired
  public StoredQueryValidation(TerminologyService terminologyService) {
    this.terminologyService = terminologyService;
  }

  /**
   * Check a stored query for invalid/outdated termcodes.
   *
   * For now, just check if the term codes still exist in the current ui profiles. Further
   * iterations may contain checking for availability of values and units of the term codes as well.
   *
   * @param storedQuery the stored query to check
   * @return a list of term codes that are no longer valid (or have never been)
   */
  public List<TermCode> getInvalidTermCodes(StoredQuery storedQuery) {
    var invalidTermCodes = new ArrayList<TermCode>();

    List<List<Criterion>> combinedCriteria = Stream.of(
        storedQuery.getStructuredQuery().getInclusionCriteria(),
        storedQuery.getStructuredQuery().getExclusionCriteria()).flatMap(
        Collection::stream).toList();

    for (List<Criterion> criterionList : combinedCriteria) {
      for (Criterion criterion : criterionList) {
        for (TermCode termCode : criterion.getTermCodes()) {
          try {
            terminologyService.getUiProfile(termCode.getSystem(),
                termCode.getCode(),
                termCode.getVersion());
            log.trace("termcode ok: {} - {} - {}", termCode.getSystem(), termCode.getCode(),
                termCode.getVersion());
          } catch (NullPointerException e) {
            // currently, terminology service throws a NPE when the code is not found
            log.debug("termcode invalid: {} - {} - {}", termCode.getSystem(), termCode.getCode(),
                termCode.getVersion());
            invalidTermCodes.add(termCode);
          }
        }
      }
    }

    return invalidTermCodes;
  }
}
