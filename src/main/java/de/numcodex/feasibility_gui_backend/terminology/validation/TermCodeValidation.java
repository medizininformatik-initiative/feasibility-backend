package de.numcodex.feasibility_gui_backend.terminology.validation;

import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
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
public class TermCodeValidation {

  private final TerminologyService terminologyService;

  @Autowired
  public TermCodeValidation(TerminologyService terminologyService) {
    this.terminologyService = terminologyService;
  }

  /**
   * Check a structured query for invalid/outdated termcodes.
   *
   * For now, just check if the term codes still exist in the current ui profiles. Further
   * iterations may contain checking for availability of values and units of the term codes as well.
   *
   * @param structuredQuery the structured query to check
   * @return a list of term codes that are no longer valid (or have never been)
   */
  public List<TermCode> getInvalidTermCodes(StructuredQuery structuredQuery) {
    var invalidTermCodes = new ArrayList<TermCode>();

    List<List<Criterion>> combinedCriteria;

    if (structuredQuery.exclusionCriteria() != null && !structuredQuery.exclusionCriteria().isEmpty()) {
      combinedCriteria = Stream.of(
          structuredQuery.inclusionCriteria(),
          structuredQuery.exclusionCriteria()).flatMap(
          Collection::stream).toList();
    } else {
      combinedCriteria = structuredQuery.inclusionCriteria();
    }

    for (List<Criterion> criterionList : combinedCriteria) {
      for (Criterion criterion : criterionList) {
        for (TermCode termCode : criterion.termCodes()) {
          if (terminologyService.isExistingTermCode(termCode.system(), termCode.code(), termCode.version())) {
            log.trace("termcode ok: {} - {} - {}", termCode.system(), termCode.code(), termCode.version());
          } else {
            log.debug("termcode invalid: {} - {} - {}", termCode.system(), termCode.code(),
                    termCode.version());
            invalidTermCodes.add(termCode);
          }
        }
      }
    }

    return invalidTermCodes;
  }
}
