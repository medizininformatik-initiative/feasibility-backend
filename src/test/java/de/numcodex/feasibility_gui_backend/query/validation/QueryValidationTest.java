package de.numcodex.feasibility_gui_backend.query.validation;

import static de.numcodex.feasibility_gui_backend.common.api.Comparator.GREATER_EQUAL;
import static de.numcodex.feasibility_gui_backend.query.api.ValueFilterType.QUANTITY_COMPARATOR;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.Unit;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.TimeRestriction;
import de.numcodex.feasibility_gui_backend.query.api.ValueFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.validation.ConstraintValidatorContext;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("query")
@Tag("validation")
public class QueryValidationTest {

  public static QueryValidator validator;

  private static ObjectMapper jsonUtil;

  private ConstraintValidatorContext constraintValidatorContext = mock(
      ConstraintValidatorContext.class);

  @BeforeAll
  public static void setUp() throws IOException {
    jsonUtil = new ObjectMapper();
    try (InputStream inputStream = QueryValidator.class.getResourceAsStream(
        "/query/query-schema.json")) {
      var jsonSchema = new JSONObject(new JSONTokener(inputStream));
      SchemaLoader loader = SchemaLoader.builder()
          .schemaJson(jsonSchema)
          .draftV7Support()
          .build();
      var schema = loader.load().build();
      validator = new QueryValidator(schema, jsonUtil);
    } catch (IOException e) {
      throw (e);
    }
  }

  @Test
  public void testValidate_validQueryOk() throws JsonProcessingException {
    var structuredQuery = buildValidQuery();
    assertTrue(validator.isValid(structuredQuery, constraintValidatorContext));
  }

  @Test
  public void testValidate_invalidQueriesFail() throws JsonProcessingException {
    var queryWithoutVersion = buildValidQuery();
    queryWithoutVersion.setVersion(null);
    assertFalse(validator.isValid(queryWithoutVersion, constraintValidatorContext));

    var queryWithoutInclusionCriteria = buildValidQuery();
    queryWithoutInclusionCriteria.setInclusionCriteria(null);
    assertFalse(validator.isValid(queryWithoutInclusionCriteria, constraintValidatorContext));

    var queryWithEmptyInclusionCriteria = buildValidQuery();
    queryWithEmptyInclusionCriteria.setInclusionCriteria(new ArrayList<>());
    assertFalse(validator.isValid(queryWithEmptyInclusionCriteria, constraintValidatorContext));

    var queryWithEmptyCriterionTermCodes = buildValidQuery();
    queryWithEmptyCriterionTermCodes.getInclusionCriteria().forEach(ic -> {
      ic.forEach(c -> {
        c.setTermCodes(null);
      });
    });
    assertFalse(validator.isValid(queryWithEmptyCriterionTermCodes, constraintValidatorContext));

    var queryWithEmptyTermCodeCodes = buildValidQuery();
    queryWithEmptyTermCodeCodes.getInclusionCriteria().forEach(ic -> {
      ic.forEach(c -> {
        c.getTermCodes().forEach(tc -> {
          tc.setCode(null);
        });
      });
    });
    assertFalse(validator.isValid(queryWithEmptyTermCodeCodes, constraintValidatorContext));

    var queryWithEmptyTermCodeSystems = buildValidQuery();
    queryWithEmptyTermCodeSystems.getInclusionCriteria().forEach(ic -> {
      ic.forEach(c -> {
        c.getTermCodes().forEach(tc -> {
          tc.setSystem(null);
        });
      });
    });
    assertFalse(validator.isValid(queryWithEmptyTermCodeSystems, constraintValidatorContext));

    var queryWithEmptyTermCodeDisplays = buildValidQuery();
    queryWithEmptyTermCodeDisplays.getInclusionCriteria().forEach(ic -> {
      ic.forEach(c -> {
        c.getTermCodes().forEach(tc -> {
          tc.setDisplay(null);
        });
      });
    });
    assertFalse(validator.isValid(queryWithEmptyTermCodeDisplays, constraintValidatorContext));

    var queryWithMalformedTimeRestrictions = buildValidQuery();
    queryWithMalformedTimeRestrictions.getInclusionCriteria().forEach(ic -> {
      ic.forEach(c -> {
        c.getTimeRestriction().setBeforeDate("foo");
        c.getTimeRestriction().setAfterDate("bar");
      });
    });
    assertFalse(validator.isValid(queryWithMalformedTimeRestrictions, constraintValidatorContext));

    var queryWithTimeRestrictionsWithoutDates = buildValidQuery();
    queryWithTimeRestrictionsWithoutDates.getInclusionCriteria().forEach(ic -> {
      ic.forEach(c -> {
        c.getTimeRestriction().setBeforeDate(null);
        c.getTimeRestriction().setAfterDate(null);
      });
    });
    assertFalse(
        validator.isValid(queryWithTimeRestrictionsWithoutDates, constraintValidatorContext));

    var queryWithMissingValueFilterType = buildValidQuery();
    queryWithMissingValueFilterType.getInclusionCriteria().forEach(ic -> {
      ic.forEach(c -> {
        c.getValueFilter().setType(null);
      });
    });
    assertFalse(validator.isValid(queryWithMissingValueFilterType, constraintValidatorContext));
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
