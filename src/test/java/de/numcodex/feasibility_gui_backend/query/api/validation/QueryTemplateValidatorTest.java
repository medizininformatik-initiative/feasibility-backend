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
    assertTrue(validator.isValid(queryWithoutStructuredQuery, constraintValidatorContext));

    var queryWithMalformedStructuredQuery = buildInvalidQueryWithMalformedStructuredQuery();
    assertFalse(validator.isValid(queryWithMalformedStructuredQuery, constraintValidatorContext));
  }

  private QueryTemplate buildValidQuery() {
    var bodyWeightTermCode = TermCode.builder()
            .code("27113001")
            .system("http://snomed.info/sct")
            .version("v1")
            .display("Body weight (observable entity)")
            .build();
    var kgUnit = Unit.builder()
            .code("kg")
            .display("kilogram")
            .build();
    var bodyWeightValueFilter = ValueFilter.builder()
            .type(QUANTITY_COMPARATOR)
            .comparator(GREATER_EQUAL)
            .quantityUnit(kgUnit)
            .value(50.0)
            .build();
    var timeRestriction = TimeRestriction.builder()
            .afterDate("2021-01-01")
            .beforeDate("2021-12-31")
            .build();

    var hasBmiGreaterThanFifty = Criterion.builder()
            .termCodes(List.of(bodyWeightTermCode))
            .valueFilter(bodyWeightValueFilter)
            .timeRestriction(timeRestriction)
            .build();
    var testQuery = StructuredQuery.builder()
            .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
            .inclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
            .exclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)))
            .display(null)
            .build();
    return QueryTemplate.builder()
            .id(0)
            .content(testQuery)
            .label("testquery")
            .comment("this is just a test query")
            .createdBy("foo-bar-1234")
            .build();
  }

  private QueryTemplate buildInvalidValidQueryWithoutLabel() {
    var validQuery = buildValidQuery();
    return QueryTemplate.builder()
            .id(validQuery.id())
            .content(validQuery.content())
            .comment(validQuery.comment())
            .lastModified(validQuery.lastModified())
            .createdBy(validQuery.createdBy())
            .invalidTerms(validQuery.invalidTerms())
            .isValid(validQuery.isValid())
            .build();
  }

  private QueryTemplate buildInvalidValidQueryWithoutContent() {
    var validQuery = buildValidQuery();
    return QueryTemplate.builder()
            .id(validQuery.id())
            .label(validQuery.label())
            .comment(validQuery.comment())
            .lastModified(validQuery.lastModified())
            .createdBy(validQuery.createdBy())
            .invalidTerms(validQuery.invalidTerms())
            .isValid(validQuery.isValid())
            .build();
  }

  private QueryTemplate buildInvalidQueryWithMalformedStructuredQuery() {
    var validQuery = buildValidQuery();
    var invalidTestQuery = StructuredQuery.builder()
            .version(validQuery.content().version())
            .exclusionCriteria(validQuery.content().exclusionCriteria())
            .display(validQuery.content().display())
            .build();
    return QueryTemplate.builder()
            .id(validQuery.id())
            .content(invalidTestQuery)
            .label(validQuery.label())
            .comment(validQuery.comment())
            .lastModified(validQuery.lastModified())
            .createdBy(validQuery.createdBy())
            .invalidTerms(validQuery.invalidTerms())
            .isValid(validQuery.isValid())
            .build();
  }

}
