package de.numcodex.feasibility_gui_backend.query.api.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.Unit;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.TimeRestriction;
import de.numcodex.feasibility_gui_backend.query.api.ValueFilter;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintValidatorContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static de.numcodex.feasibility_gui_backend.common.api.Comparator.GREATER_EQUAL;
import static de.numcodex.feasibility_gui_backend.query.api.ValueFilterType.QUANTITY_COMPARATOR;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("query")
@Tag("api")
@Tag("validation")
@ExtendWith(MockitoExtension.class)
public class StructuredQueryValidatorTest {
  public static StructuredQueryValidator validator;

  @Mock
  private ConstraintValidatorContext constraintValidatorContext;

  @BeforeAll
  public static void setUp() throws IOException {
    var jsonUtil = new ObjectMapper();
    InputStream inputStream = StructuredQueryValidator.class.getResourceAsStream(
        "/de/numcodex/feasibility_gui_backend/query/api/validation/query-schema.json");
    var jsonSchema = new JSONObject(new JSONTokener(inputStream));
    SchemaLoader loader = SchemaLoader.builder()
        .schemaJson(jsonSchema)
        .draftV7Support()
        .build();
    var schema = loader.load().build();
    validator = new StructuredQueryValidator(schema, jsonUtil);
  }

  @Test
  public void testValidate_validQueryOk() {
    var structuredQuery = buildValidQuery();
    assertTrue(validator.isValid(structuredQuery, constraintValidatorContext));
  }

  @Test
  public void testValidate_invalidQueriesFail() {
    var queryWithoutVersion = buildInvalidQueryWithoutVersion();
    assertFalse(validator.isValid(queryWithoutVersion, constraintValidatorContext));

    var queryWithoutInclusionCriteria = buildInvalidQueryWithoutInclusionCriteria();
    assertFalse(validator.isValid(queryWithoutInclusionCriteria, constraintValidatorContext));

    var queryWithEmptyInclusionCriteria = buildInvalidQueryWithEmptyInclusionCriteria();
    assertFalse(validator.isValid(queryWithEmptyInclusionCriteria, constraintValidatorContext));

    var queryWithEmptyCriterionTermCodes = buildInvalidQueryWithEmptyTermCodes();
    assertFalse(validator.isValid(queryWithEmptyCriterionTermCodes, constraintValidatorContext));

    var queryWithEmptyTermCodeCodes = buildInvalidQueryWithEmptyTermCodeCodes();
    assertFalse(validator.isValid(queryWithEmptyTermCodeCodes, constraintValidatorContext));

    var queryWithEmptyTermCodeSystems = buildInvalidQueryWithEmptyTermCodeSystems();
    assertFalse(validator.isValid(queryWithEmptyTermCodeSystems, constraintValidatorContext));

    var queryWithEmptyTermCodeDisplays = buildInvalidQueryWithEmptyTermCodeDisplays();
    assertFalse(validator.isValid(queryWithEmptyTermCodeDisplays, constraintValidatorContext));

    var queryWithMalformedTimeRestrictions = buildInvalidQueryWithMalformedTimeRestrictions();
    assertFalse(validator.isValid(queryWithMalformedTimeRestrictions, constraintValidatorContext));

    var queryWithTimeRestrictionsWithoutDates = buildInvalidQueryWithTimeRestrictionsWithoutDates();
    assertFalse(
        validator.isValid(queryWithTimeRestrictionsWithoutDates, constraintValidatorContext));
  }

  private StructuredQuery buildValidQuery() {
    var bodyWeightTermCode = new TermCode("27113001", "http://snomed.info/sct", "v1", "Body weight (observable entity)");
    var kgUnit = new Unit("kg", "kilogram");
    var bodyWeightValueFilter = new ValueFilter(QUANTITY_COMPARATOR, null, GREATER_EQUAL, kgUnit,
        50.0, null, null);
    var timeRestriction = new TimeRestriction("2021-12-31", "2021-01-01");
    var hasBmiGreaterThanFifty = new Criterion(List.of(bodyWeightTermCode), null,
        bodyWeightValueFilter, timeRestriction);
    return new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"),
        List.of(List.of(hasBmiGreaterThanFifty)), List.of(List.of(hasBmiGreaterThanFifty)), null);
  }

  private StructuredQuery buildInvalidQueryWithoutVersion() {
    var validQuery = buildValidQuery();
    return new StructuredQuery(
      null,
      validQuery.inclusionCriteria(),
      validQuery.exclusionCriteria(),
        validQuery.display()
    );
  }

  private StructuredQuery buildInvalidQueryWithoutInclusionCriteria() {
    var validQuery = buildValidQuery();
    return new StructuredQuery(
        validQuery.version(),
        null,
        validQuery.exclusionCriteria(),
        validQuery.display()
    );
  }

  private StructuredQuery buildInvalidQueryWithEmptyInclusionCriteria() {
    var validQuery = buildValidQuery();
    return new StructuredQuery(
        validQuery.version(),
        List.of(),
        validQuery.exclusionCriteria(),
        validQuery.display()
    );
  }

  private StructuredQuery buildInvalidQueryWithEmptyTermCodes() {
    var bodyWeightTermCode = new TermCode(null, null, null, null);
    var kgUnit = new Unit("kg", "kilogram");
    var bodyWeightValueFilter = new ValueFilter(QUANTITY_COMPARATOR, null, GREATER_EQUAL, kgUnit,
        50.0, null, null);
    var timeRestriction = new TimeRestriction("2021-12-31", "2021-01-01");
    var hasBmiGreaterThanFifty = new Criterion(List.of(bodyWeightTermCode), null,
        bodyWeightValueFilter, timeRestriction);
    return new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"),
        List.of(List.of(hasBmiGreaterThanFifty)), List.of(List.of(hasBmiGreaterThanFifty)), null);
  }

  private StructuredQuery buildInvalidQueryWithEmptyTermCodeCodes() {
    var bodyWeightTermCode = new TermCode(null, "http://snomed.info/sct", "v1", "Body weight (observable entity)");
    var kgUnit = new Unit("kg", "kilogram");
    var bodyWeightValueFilter = new ValueFilter(QUANTITY_COMPARATOR, null, GREATER_EQUAL, kgUnit,
        50.0, null, null);
    var timeRestriction = new TimeRestriction("2021-12-31", "2021-01-01");
    var hasBmiGreaterThanFifty = new Criterion(List.of(bodyWeightTermCode), null,
        bodyWeightValueFilter, timeRestriction);
    return new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"),
        List.of(List.of(hasBmiGreaterThanFifty)), List.of(List.of(hasBmiGreaterThanFifty)), null);
  }

  private StructuredQuery buildInvalidQueryWithEmptyTermCodeSystems() {
    var bodyWeightTermCode = new TermCode("27113001", null, "v1", "Body weight (observable entity)");
    var kgUnit = new Unit("kg", "kilogram");
    var bodyWeightValueFilter = new ValueFilter(QUANTITY_COMPARATOR, null, GREATER_EQUAL, kgUnit,
        50.0, null, null);
    var timeRestriction = new TimeRestriction("2021-12-31", "2021-01-01");
    var hasBmiGreaterThanFifty = new Criterion(List.of(bodyWeightTermCode), null,
        bodyWeightValueFilter, timeRestriction);
    return new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"),
        List.of(List.of(hasBmiGreaterThanFifty)), List.of(List.of(hasBmiGreaterThanFifty)), null);
  }

  private StructuredQuery buildInvalidQueryWithEmptyTermCodeDisplays() {
    var bodyWeightTermCode = new TermCode("27113001", "http://snomed.info/sct", "v1", null);
    var kgUnit = new Unit("kg", "kilogram");
    var bodyWeightValueFilter = new ValueFilter(QUANTITY_COMPARATOR, null, GREATER_EQUAL, kgUnit,
        50.0, null, null);
    var timeRestriction = new TimeRestriction("2021-12-31", "2021-01-01");
    var hasBmiGreaterThanFifty = new Criterion(List.of(bodyWeightTermCode), null,
        bodyWeightValueFilter, timeRestriction);
    return new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"),
        List.of(List.of(hasBmiGreaterThanFifty)), List.of(List.of(hasBmiGreaterThanFifty)), null);
  }

  private StructuredQuery buildInvalidQueryWithMalformedTimeRestrictions() {
    var bodyWeightTermCode = new TermCode("27113001", "http://snomed.info/sct", "v1", "Body weight (observable entity)");
    var kgUnit = new Unit("kg", "kilogram");
    var bodyWeightValueFilter = new ValueFilter(QUANTITY_COMPARATOR, null, GREATER_EQUAL, kgUnit,
        50.0, null, null);
    var timeRestriction = new TimeRestriction("foo", "bar");
    var hasBmiGreaterThanFifty = new Criterion(List.of(bodyWeightTermCode), null,
        bodyWeightValueFilter, timeRestriction);
    return new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"),
        List.of(List.of(hasBmiGreaterThanFifty)), List.of(List.of(hasBmiGreaterThanFifty)), null);
  }

  private StructuredQuery buildInvalidQueryWithTimeRestrictionsWithoutDates() {
    var bodyWeightTermCode = new TermCode("27113001", "http://snomed.info/sct", "v1", "Body weight (observable entity)");
    var kgUnit = new Unit("kg", "kilogram");
    var bodyWeightValueFilter = new ValueFilter(QUANTITY_COMPARATOR, null, GREATER_EQUAL, kgUnit,
        50.0, null, null);
    var timeRestriction = new TimeRestriction(null, null);
    var hasBmiGreaterThanFifty = new Criterion(List.of(bodyWeightTermCode), null,
        bodyWeightValueFilter, timeRestriction);
    return new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"),
        List.of(List.of(hasBmiGreaterThanFifty)), List.of(List.of(hasBmiGreaterThanFifty)), null);
  }

}
