package de.numcodex.feasibility_gui_backend.query.api.validation;

import static de.numcodex.feasibility_gui_backend.common.api.Comparator.GREATER_EQUAL;
import static de.numcodex.feasibility_gui_backend.query.api.ValueFilterType.QUANTITY_COMPARATOR;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.Unit;
import de.numcodex.feasibility_gui_backend.query.api.QueryTemplate;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.TimeRestriction;
import de.numcodex.feasibility_gui_backend.query.api.ValueFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import jakarta.validation.ConstraintValidatorContext;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("query")
@Tag("api")
@Tag("validation-template")
@ExtendWith(MockitoExtension.class)
public class QueryTemplateValidatorTest {
  public static QueryTemplateValidator validator;

  @Mock
  private ConstraintValidatorContext constraintValidatorContext;

  @BeforeAll
  public static void setUp() throws IOException {
    var jsonUtil = new ObjectMapper();
    InputStream inputStream = QueryTemplateValidator.class.getResourceAsStream(
        "/de/numcodex/feasibility_gui_backend/query/api/validation/query-template-schema.json");
    var jsonSchema = new JSONObject(new JSONTokener(inputStream));
    SchemaLoader loader = SchemaLoader.builder()
        .schemaClient(SchemaClient.classPathAwareClient())
        .schemaJson(jsonSchema)
        .resolutionScope("classpath://de/numcodex/feasibility_gui_backend/query/api/validation/")
        .draftV7Support()
        .build();
    var schema = loader.load().build();
    validator = new QueryTemplateValidator(schema, jsonUtil);
  }

  @Test
  public void testValidate_validQueryOk() {
    var queryTemplate = buildValidQuery();
    assertTrue(validator.isValid(queryTemplate, constraintValidatorContext));
  }

  @Test
  public void testValidate_invalidQueriesFail() {
    var queryWithoutLabel = buildInvalidValidQueryWithoutLabel();
    assertFalse(validator.isValid(queryWithoutLabel, constraintValidatorContext));

    var queryWithoutStructuredQuery = buildInvalidValidQueryWithoutContent();
    assertFalse(validator.isValid(queryWithoutStructuredQuery, constraintValidatorContext));

    var queryWithMalformedStructuredQuery = buildInvalidQueryWithMalformedStructuredQuery();
    assertFalse(validator.isValid(queryWithMalformedStructuredQuery, constraintValidatorContext));
  }

  private QueryTemplate buildValidQuery() {
    var bodyWeightTermCode = new TermCode("27113001", "http://snomed.info/sct", "v1",
        "Body weight (observable entity)");
    var kgUnit = new Unit("kg", "kilogram");
    var bodyWeightValueFilter = new ValueFilter(QUANTITY_COMPARATOR, null, GREATER_EQUAL, kgUnit,
        50.0, null, null);
    var timeRestriction = new TimeRestriction("2021-12-31", "2021-01-01");
    var hasBmiGreaterThanFifty = new Criterion(List.of(bodyWeightTermCode), null,
        bodyWeightValueFilter, timeRestriction);
    var testQuery = new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"),
        List.of(List.of(hasBmiGreaterThanFifty)), List.of(List.of(hasBmiGreaterThanFifty)), null);
    return new QueryTemplate(0, testQuery, "testquery", "this is just a test query", null,
        "foo-bar-1234", null, null);
  }

  private QueryTemplate buildInvalidValidQueryWithoutLabel() {
    var validQuery = buildValidQuery();
    return new QueryTemplate(validQuery.id(),
        validQuery.content(),
        null,
        validQuery.comment(),
        validQuery.lastModified(),
        validQuery.createdBy(),
        validQuery.invalidTerms(),
        validQuery.isValid());
  }

  private QueryTemplate buildInvalidValidQueryWithoutContent() {
    var validQuery = buildValidQuery();
    return new QueryTemplate(validQuery.id(),
        null,
        validQuery.label(),
        validQuery.comment(),
        validQuery.lastModified(),
        validQuery.createdBy(),
        validQuery.invalidTerms(),
        validQuery.isValid());
  }

  private QueryTemplate buildInvalidQueryWithMalformedStructuredQuery() {
    var validQuery = buildValidQuery();
    var invalidTestQuery = new StructuredQuery(validQuery.content().version(), null, validQuery.content()
        .exclusionCriteria(), validQuery.content().display());
    return new QueryTemplate(validQuery.id(),
        invalidTestQuery,
        validQuery.label(),
        validQuery.comment(),
        validQuery.lastModified(),
        validQuery.createdBy(),
        validQuery.invalidTerms(),
        validQuery.isValid());
  }

}
