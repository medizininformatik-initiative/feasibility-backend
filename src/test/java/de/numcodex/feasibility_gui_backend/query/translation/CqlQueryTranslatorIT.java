package de.numcodex.feasibility_gui_backend.query.translation;

import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.Unit;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.query.api.AttributeFilter;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.TimeRestriction;
import de.numcodex.feasibility_gui_backend.query.api.ValueFilter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;
import java.util.List;

import static de.numcodex.feasibility_gui_backend.common.api.Comparator.GREATER_EQUAL;
import static de.numcodex.feasibility_gui_backend.common.api.Comparator.GREATER_THAN;
import static de.numcodex.feasibility_gui_backend.query.api.ValueFilterType.CONCEPT;
import static de.numcodex.feasibility_gui_backend.query.api.ValueFilterType.QUANTITY_COMPARATOR;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Tag("query")
@Tag("translation")
@Disabled("Until we have a version of sq2cql that actually supports translating a structured query v2.")
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = QueryTranslatorSpringConfig.class,
        properties = {
                "app.cqlTranslationEnabled=true",
                "app.fhirTranslationEnabled=false",
                "app.mappingsFile=./ontology/codex-term-code-mapping.json",
                "app.conceptTreeFile=./ontology/codex-code-tree.json"
        }
)
@SuppressWarnings("NewClassNamingConvention")
public class CqlQueryTranslatorIT {

    @Autowired
    @Qualifier("cql")
    private QueryTranslator cqlQueryTranslator;

    @Test
    public void testTranslate() {
        var bodyWeightTermCode = new TermCode("27113001", "http://snomed.info/sct", "v1", "Body weight (observable entity)");
        var kgUnit = new Unit("kg", "kilogram");
        var bodyWeightValueFilter = new ValueFilter(QUANTITY_COMPARATOR, null, GREATER_EQUAL, kgUnit,
            50.0, null, null);
        var hasBmiGreaterThanFifty = new Criterion(List.of(bodyWeightTermCode), null,
            bodyWeightValueFilter, null);
        var testQuery = new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"), List.of(List.of(hasBmiGreaterThanFifty)), null, null);

        @SuppressWarnings("unused")
        var translationResult = assertDoesNotThrow(() -> cqlQueryTranslator.translate(testQuery));
        // TODO: add assertions!
    }

    @Test
    public void testTranslate_SupportsTimeRestrictions() {
        var dementiaTermCode = new TermCode("F00", "http://fhir.de/CodeSystem/dimdi/icd-10-gm", null, "F00");
        var hasDementia = new Criterion(List.of(dementiaTermCode), null, null, null);
        var psychologicalDysfunctionTermCode = new TermCode("F09", "http://fhir.de/CodeSystem/dimdi/icd-10-gm", null, "F09");
        var timeRestriction = new TimeRestriction("2021-10-09", "2021-09-09");
        var hasPsychologicalDysfunction = new Criterion(List.of(psychologicalDysfunctionTermCode), null, null, timeRestriction);
        var testQuery = new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"), List.of(List.of(hasDementia, hasPsychologicalDysfunction)), null, null);

        @SuppressWarnings("unused")
        var translationResult = assertDoesNotThrow(() -> cqlQueryTranslator.translate(testQuery));
        // TODO: add assertions!
    }

    @Test
    public void testTranslate_SupportsAttributeFilters() {
        var ageTermCode = new TermCode("30525-0", "http://loinc.org", null, "Alter");
        var yearUnit = new Unit("a", "Jahr");
        var ageValueFilter = new ValueFilter(QUANTITY_COMPARATOR, null, GREATER_THAN, yearUnit, 18.0, null, null);
        var olderThanEighteen = new Criterion(List.of(ageTermCode), null, ageValueFilter, null);
        var bodyTemperatureTermCode = new TermCode("8310-5", "http://loinc.org", null, "Körpertemperatur");
        var axillaryMeasureMethod = new TermCode("LA9370-3", "http://loinc.org", null, "Axillary");
        var method = new TermCode("method", "abide", null, "method");
        var axillaryMeasured = new AttributeFilter(CONCEPT, List.of(axillaryMeasureMethod), null, null, null, null, null, method);
        var bodyTemperatureAxillaryMeasured = new Criterion(List.of(bodyTemperatureTermCode), List.of(axillaryMeasured), null, null);
        var testQuery = new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"), List.of(List.of(olderThanEighteen)), List.of(List.of(bodyTemperatureAxillaryMeasured)), null);

        @SuppressWarnings("unused")
        var translationResult = assertDoesNotThrow(() -> cqlQueryTranslator.translate(testQuery));
        // TODO: add assertions!
    }
}
