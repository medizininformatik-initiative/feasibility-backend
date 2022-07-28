package de.numcodex.feasibility_gui_backend.query.api.validation;

import static de.numcodex.feasibility_gui_backend.common.api.Comparator.GREATER_EQUAL;
import static de.numcodex.feasibility_gui_backend.query.api.ValueFilterType.QUANTITY_COMPARATOR;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.Unit;
import de.numcodex.feasibility_gui_backend.query.api.StoredQuery;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.TimeRestriction;
import de.numcodex.feasibility_gui_backend.query.api.ValueFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.validation.ConstraintValidatorContext;
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
@Tag("validation-stored")
@ExtendWith(MockitoExtension.class)
public class StoredQueryValidatorTest {
  public static StoredQueryValidator validator;

  @Mock
  private ConstraintValidatorContext constraintValidatorContext;

  @BeforeAll
  public static void setUp() throws IOException {
    var jsonUtil = new ObjectMapper();
    InputStream inputStream = StoredQueryValidator.class.getResourceAsStream(
        "/query/stored-query-schema.json");
    var jsonSchema = new JSONObject(new JSONTokener(inputStream));
    SchemaLoader loader = SchemaLoader.builder()
        .schemaClient(SchemaClient.classPathAwareClient())
        .schemaJson(jsonSchema)
        .resolutionScope("classpath://query/")
        .draftV7Support()
        .build();
    var schema = loader.load().build();
    validator = new StoredQueryValidator(schema, jsonUtil);
  }

  @Test
  public void testValidate_validQueryOk() {
    var storedQuery = buildValidQuery();
    assertTrue(validator.isValid(storedQuery, constraintValidatorContext));
  }

  @Test
  public void testValidate_invalidQueriesFail() {
    var queryWithoutLabel = buildValidQuery();
    queryWithoutLabel.setLabel(null);
    assertFalse(validator.isValid(queryWithoutLabel, constraintValidatorContext));

    var queryWithoutStructuredQuery = buildValidQuery();
    queryWithoutStructuredQuery.setStructuredQuery(null);
    assertFalse(validator.isValid(queryWithoutStructuredQuery, constraintValidatorContext));

    var queryWithMalformedStructuredQuery = buildValidQuery();
    var malformedStructuredQuery = queryWithMalformedStructuredQuery.getStructuredQuery();
    malformedStructuredQuery.setInclusionCriteria(null);
    queryWithMalformedStructuredQuery.setStructuredQuery(malformedStructuredQuery);
    assertFalse(validator.isValid(queryWithMalformedStructuredQuery, constraintValidatorContext));
  }

  private StoredQuery buildValidQuery() {
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

    var testStoredQuery = new StoredQuery();
    testStoredQuery.setStructuredQuery(testQuery);
    testStoredQuery.setLabel("testquery");
    testStoredQuery.setComment("this is just a test query");
    testStoredQuery.setCreatedBy("foo-bar-1234");

    return testStoredQuery;
  }

}
