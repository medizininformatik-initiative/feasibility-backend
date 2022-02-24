package de.numcodex.feasibility_gui_backend.query.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.Unit;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.TimeRestriction;
import de.numcodex.feasibility_gui_backend.query.api.ValueFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.everit.json.schema.ValidationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static de.numcodex.feasibility_gui_backend.common.api.Comparator.GREATER_EQUAL;
import static de.numcodex.feasibility_gui_backend.query.api.ValueFilterType.QUANTITY_COMPARATOR;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Tag("query")
@Tag("validation")
public class QueryValidationTest {

  public static QueryValidator validator;

  @BeforeAll
  public static void setUp() {
    var jsonUtil = new ObjectMapper();
    validator = new QueryValidator(jsonUtil);
  }

  @Test
  public void testValidate_validQueryOk() {
    var structuredQuery = buildValidQuery();
    assertDoesNotThrow(() -> validator.validate(structuredQuery));
  }

  @Test
  public void testValidate_invalidQueriesThrow() {
    var queryWithoutVersion = buildValidQuery();
    queryWithoutVersion.setVersion(null);
    assertThrows(ValidationException.class, () -> validator.validate(queryWithoutVersion));

    var queryWithoutInclusionCriteria = buildValidQuery();
    queryWithoutInclusionCriteria.setInclusionCriteria(null);
    assertThrows(ValidationException.class, () -> validator.validate(queryWithoutInclusionCriteria));

    var queryWithEmptyInclusionCriteria = buildValidQuery();
    queryWithEmptyInclusionCriteria.setInclusionCriteria(new ArrayList<>());
    assertThrows(ValidationException.class, () -> validator.validate(queryWithEmptyInclusionCriteria));

    var queryWithEmptyCriterionTermCodes = buildValidQuery();
    queryWithEmptyCriterionTermCodes.getInclusionCriteria().forEach(ic -> {
      ic.forEach(c -> {
        c.setTermCodes(null);
      });
    });
    assertThrows(ValidationException.class, () -> validator.validate(queryWithEmptyCriterionTermCodes));

    var queryWithEmptyTermCodeCodes = buildValidQuery();
    queryWithEmptyTermCodeCodes.getInclusionCriteria().forEach(ic -> {
      ic.forEach(c -> {
        c.getTermCodes().forEach(tc -> {
          tc.setCode(null);
        });
      });
    });
    assertThrows(ValidationException.class, () -> validator.validate(queryWithEmptyTermCodeCodes));

    var queryWithEmptyTermCodeSystems = buildValidQuery();
    queryWithEmptyTermCodeSystems.getInclusionCriteria().forEach(ic -> {
      ic.forEach(c -> {
        c.getTermCodes().forEach(tc -> {
          tc.setSystem(null);
        });
      });
    });
    assertThrows(ValidationException.class, () -> validator.validate(queryWithEmptyTermCodeSystems));

    var queryWithEmptyTermCodeDisplays = buildValidQuery();
    queryWithEmptyTermCodeDisplays.getInclusionCriteria().forEach(ic -> {
      ic.forEach(c -> {
        c.getTermCodes().forEach(tc -> {
          tc.setDisplay(null);
        });
      });
    });
    assertThrows(ValidationException.class, () -> validator.validate(queryWithEmptyTermCodeDisplays));

    var queryWithMalformedTimeRestrictions = buildValidQuery();
    queryWithMalformedTimeRestrictions.getInclusionCriteria().forEach(ic -> {
      ic.forEach(c -> {
        c.getTimeRestriction().setBeforeDate("foo");
        c.getTimeRestriction().setAfterDate("bar");
      });
    });
    assertThrows(ValidationException.class, () -> validator.validate(queryWithMalformedTimeRestrictions));

    var queryWithTimeRestrictionsWithoutDates = buildValidQuery();
    queryWithTimeRestrictionsWithoutDates.getInclusionCriteria().forEach(ic -> {
      ic.forEach(c -> {
        c.getTimeRestriction().setBeforeDate(null);
        c.getTimeRestriction().setAfterDate(null);
      });
    });
    assertThrows(ValidationException.class, () -> validator.validate(queryWithTimeRestrictionsWithoutDates));

    var queryWithMissingValueFilterType = buildValidQuery();
    queryWithMissingValueFilterType.getInclusionCriteria().forEach(ic -> {
      ic.forEach(c -> {
        c.getValueFilter().setType(null);
      });
    });
    assertThrows(ValidationException.class, () -> validator.validate(queryWithMissingValueFilterType));
  }

  private StructuredQuery buildValidQuery() {
    var bodyWeightTermCode = new TermCode();
    bodyWeightTermCode.setSystem("http://snomed.info/sct");
    bodyWeightTermCode.setDisplay("Body weight (observable entity)");
    bodyWeightTermCode.setCode("27113001");
    bodyWeightTermCode.setVersion("v1");

    var kgUnit = new Unit();
    kgUnit.setCode("kg");
    kgUnit.setDisplay("kilogram");

    var bodyWeightValueFilter = new ValueFilter();
    bodyWeightValueFilter.setType(QUANTITY_COMPARATOR);
    bodyWeightValueFilter.setQuantityUnit(kgUnit);
    bodyWeightValueFilter.setComparator(GREATER_EQUAL);
    bodyWeightValueFilter.setValue(50.0);

    var hasBmiGreaterThanFifty = new Criterion();
    hasBmiGreaterThanFifty.setTermCodes(new ArrayList<>(List.of(bodyWeightTermCode)));
    hasBmiGreaterThanFifty.setValueFilter(bodyWeightValueFilter);

    var timeRestriction = new TimeRestriction();
    timeRestriction.setAfterDate("2021-01-01");
    timeRestriction.setBeforeDate("2021-12-31");

    hasBmiGreaterThanFifty.setTimeRestriction(timeRestriction);

    var testQuery = new StructuredQuery();
    testQuery.setVersion(URI.create("http://to_be_decided.com/draft-2/schema#"));
    testQuery.setInclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)));
    testQuery.setExclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)));

    return testQuery;
  }

}
