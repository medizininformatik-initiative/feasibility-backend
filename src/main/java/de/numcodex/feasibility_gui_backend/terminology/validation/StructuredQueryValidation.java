package de.numcodex.feasibility_gui_backend.terminology.validation;

import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.MutableCriterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.MutableStructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.status.ValidationIssue;
import de.numcodex.feasibility_gui_backend.terminology.TerminologyService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StructuredQueryValidation {

  private final TerminologyService terminologyService;

  @Autowired
  public StructuredQueryValidation(TerminologyService terminologyService) {
    this.terminologyService = terminologyService;
  }

  /**
   * Check a structured query for invalid/outdated termcodes in criteria and annotate it with the issues.
   *
   * For now, just check if the term codes still exist in the current ui profiles. Further
   * iterations may contain checking for availability of values and units of the term codes as well.
   *
   * @param structuredQuery the structured query to check
   * @return the structuredQuery with issue annotation
   */
  public StructuredQuery annotateStructuredQuery(StructuredQuery structuredQuery) {
    var mutableStructuredQuery = MutableStructuredQuery.createMutableStructuredQuery(structuredQuery);

    for (List<MutableCriterion> inclusionCriteria : mutableStructuredQuery.getInclusionCriteria()) {
      annotateCriteria(inclusionCriteria);
    }

    for (List<MutableCriterion> exclusionCriteria : mutableStructuredQuery.getExclusionCriteria()) {
      annotateCriteria(exclusionCriteria);
    }

    return StructuredQuery.createImmutableStructuredQuery(mutableStructuredQuery);
  }

  /**
   * Check a structured query for invalid/outdated termcodes in criteria and annotate it with the issues.
   *
   * For now, just check if the term codes still exist in the current ui profiles. Further
   * iterations may contain checking for availability of values and units of the term codes as well.
   *
   * @param structuredQuery the structured query to check
   * @return the structuredQuery with issue annotation
   */
  public boolean isValid(StructuredQuery structuredQuery) {
    if (structuredQuery.inclusionCriteria() != null) {
      for (List<Criterion> inclusionCriteria : structuredQuery.inclusionCriteria()) {
        if (containsInvalidCriteria(inclusionCriteria)) {
          return false;
        }
      }
    }

    if (structuredQuery.exclusionCriteria() != null) {
      for (List<Criterion> exclusionCriteria : structuredQuery.exclusionCriteria()) {
        if (containsInvalidCriteria(exclusionCriteria)) {
          return false;
        }
      }
    }

    return true;
  }

  private void annotateCriteria(List<MutableCriterion> criteria) {
    for (MutableCriterion criterion : criteria) {
      if (criterion.getContext() == null) {
        criterion.setValidationIssues(List.of(ValidationIssue.TERMCODE_CONTEXT_COMBINATION_INVALID));
        continue;
      }
      for (TermCode termCode : criterion.getTermCodes()) {
        if (terminologyService.isExistingTermCode(termCode.system(), termCode.code(), termCode.version())) {
          log.trace("termcode ok: {} - {} - {}", termCode.system(), termCode.code(), termCode.version());
          criterion.setValidationIssues(List.of()); // empty list is expected
        } else {
          log.debug("termcode invalid: {} - {} - {}", termCode.system(), termCode.code(),
              termCode.version());
          criterion.setValidationIssues(List.of(ValidationIssue.TERMCODE_CONTEXT_COMBINATION_INVALID));
        }
      }
    }
  }

  private boolean containsInvalidCriteria(List<Criterion> inclusionCriteria) {
    for (Criterion criterion : inclusionCriteria) {
      if (criterion.context() == null) {
        return true;
      }
      for (TermCode termCode : criterion.termCodes()) {
        if (terminologyService.isExistingTermCode(termCode.system(), termCode.code(), termCode.version())) {
          log.trace("termcode ok: {} - {} - {}", termCode.system(), termCode.code(), termCode.version());
        } else {
          log.debug("termcode invalid: {} - {} - {}", termCode.system(), termCode.code(),
              termCode.version());
          return true;
        }
      }
    }
    return false;
  }
}
