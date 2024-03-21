package de.numcodex.feasibility_gui_backend.terminology.validation;

import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.MutableCriterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.MutableStructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.status.ValidationIssue;
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
public class StructuredQueryValidation {

  private final TerminologyService terminologyService;

  @Autowired
  public StructuredQueryValidation(TerminologyService terminologyService) {
    this.terminologyService = terminologyService;
  }

  private Criterion removeFilters(Criterion in) {
    return Criterion.builder()
        .termCodes(in.termCodes())
        .context(in.context())
        .build();
  }

  /**
   * Check a structured query for invalid/outdated termcodes in criteria.
   *
   * For now, just check if the term codes still exist in the current ui profiles. Further
   * iterations may contain checking for availability of values and units of the term codes as well.
   *
   * @param structuredQuery the structured query to check
   * @return a list of criteria that are no longer valid (or have never been)
   */
  public List<Criterion> getInvalidCriteria(StructuredQuery structuredQuery) {
    var invalidCriteria = new ArrayList<Criterion>();

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
        if (criterion.context() == null) {
          log.debug("Missing context");
          invalidCriteria.add(removeFilters(criterion));
        }
        for (TermCode termCode : criterion.termCodes()) {
          if (terminologyService.isExistingTermCode(termCode.system(), termCode.code(), termCode.version())) {
            log.trace("termcode ok: {} - {} - {}", termCode.system(), termCode.code(), termCode.version());
          } else {
            log.debug("termcode invalid: {} - {} - {}", termCode.system(), termCode.code(),
                termCode.version());
            invalidCriteria.add(removeFilters(criterion));
          }
        }
      }
    }

    return invalidCriteria;
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
      for (MutableCriterion criterion : inclusionCriteria) {
        if (criterion.getContext() == null) {
          criterion.setValidationIssues(List.of(ValidationIssue.TERMCODE_CONTEXT_COMBINATION_INVALID));
          continue;
        }
        for (TermCode termCode : criterion.getTermCodes()) {
          if (terminologyService.isExistingTermCode(termCode.system(), termCode.code(), termCode.version())) {
            log.trace("termcode ok: {} - {} - {}", termCode.system(), termCode.code(), termCode.version());
          } else {
            log.debug("termcode invalid: {} - {} - {}", termCode.system(), termCode.code(),
                termCode.version());
            criterion.setValidationIssues(List.of(ValidationIssue.TERMCODE_CONTEXT_COMBINATION_INVALID));
          }
        }
      }
    }

    for (List<MutableCriterion> exclusionCriteria : mutableStructuredQuery.getExclusionCriteria()) {
      for (MutableCriterion criterion : exclusionCriteria) {
        if (criterion.getContext() == null) {
          criterion.setValidationIssues(List.of(ValidationIssue.TERMCODE_CONTEXT_COMBINATION_INVALID));
          continue;
        }
        for (TermCode termCode : criterion.getTermCodes()) {
          if (terminologyService.isExistingTermCode(termCode.system(), termCode.code(), termCode.version())) {
            log.trace("termcode ok: {} - {} - {}", termCode.system(), termCode.code(), termCode.version());
          } else {
            log.debug("termcode invalid: {} - {} - {}", termCode.system(), termCode.code(),
                termCode.version());
            criterion.setValidationIssues(List.of(ValidationIssue.TERMCODE_CONTEXT_COMBINATION_INVALID));
          }
        }
      }
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
        for (Criterion criterion : inclusionCriteria) {
          if (criterion.context() == null) {
            return false;
          }
          for (TermCode termCode : criterion.termCodes()) {
            if (terminologyService.isExistingTermCode(termCode.system(), termCode.code(), termCode.version())) {
              log.trace("termcode ok: {} - {} - {}", termCode.system(), termCode.code(), termCode.version());
            } else {
              log.debug("termcode invalid: {} - {} - {}", termCode.system(), termCode.code(),
                  termCode.version());
              return false;
            }
          }
        }
      }
    }

    if (structuredQuery.exclusionCriteria() != null) {
      for (List<Criterion> exclusionCriteria : structuredQuery.exclusionCriteria()) {
        for (Criterion criterion : exclusionCriteria) {
          if (criterion.context() == null) {
            return false;
          }
          for (TermCode termCode : criterion.termCodes()) {
            if (terminologyService.isExistingTermCode(termCode.system(), termCode.code(), termCode.version())) {
              log.trace("termcode ok: {} - {} - {}", termCode.system(), termCode.code(), termCode.version());
            } else {
              log.debug("termcode invalid: {} - {} - {}", termCode.system(), termCode.code(),
                  termCode.version());
              return false;
            }
          }
        }
      }
    }

    return true;
  }
}
