package de.numcodex.feasibility_gui_backend.query.dataquery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.*;
import de.numcodex.feasibility_gui_backend.dse.api.LocalizedValue;
import de.numcodex.feasibility_gui_backend.dse.persistence.DseProfile;
import de.numcodex.feasibility_gui_backend.dse.persistence.DseProfileRepository;
import de.numcodex.feasibility_gui_backend.query.api.*;
import de.numcodex.feasibility_gui_backend.terminology.api.CodeableConceptEntry;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResultEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.CodeableConceptService;
import de.numcodex.feasibility_gui_backend.terminology.es.TerminologyEsService;
import de.numcodex.feasibility_gui_backend.terminology.persistence.UiProfile;
import de.numcodex.feasibility_gui_backend.terminology.persistence.UiProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataqueryCsvExportServiceTest {

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private DseProfileRepository dseProfileRepository;

  @Mock
  private UiProfileRepository uiProfileRepository;

  @Mock
  private TerminologyEsService terminologyEsService;

  @Mock
  private CodeableConceptService codeableConceptService;

  @InjectMocks
  private DataqueryCsvExportService dataqueryCsvExportService;

  @ParameterizedTest
  @MethodSource("provideParamsForCriteriaTest")
  void testJsonToCsv_criteria(DataqueryCsvExportService.SUPPORTED_LANGUAGES language,
                              ValueFilterType valueFilterType,
                              boolean includeTimerestriction,
                              boolean withAfterDate,
                              boolean withBeforeDate) throws IOException {
    var structuredQuery = createValidStructuredQuery(valueFilterType, includeTimerestriction, withAfterDate, withBeforeDate);
    DisplayEntry displayEntry = createDisplayEntry();

    EsSearchResultEntry esSearchResultEntry = mock(EsSearchResultEntry.class);
    doReturn(displayEntry).when(esSearchResultEntry).display();
    doReturn(esSearchResultEntry).when(terminologyEsService).getSearchResultEntryByCriterion(any(Criterion.class));
    doReturn(Optional.of(createUiProfile())).when(uiProfileRepository).findByContextualizedTermcodeHash(any(String.class));
    doReturn(createUiProfileApi()).when(objectMapper).readValue(anyString(), eq(de.numcodex.feasibility_gui_backend.terminology.api.UiProfile.class));

    String csvResult = dataqueryCsvExportService.jsonToCsv(structuredQuery.inclusionCriteria(), language);

    assertNotNull(csvResult);
    switch (language) {
      case EN:
        assertTrue(csvResult.contains(displayEntry.translations().get(1).value()));
        break;
      case DE:
        assertTrue(csvResult.contains(displayEntry.translations().get(0).value()));
         break;
    }
  }

  @ParameterizedTest
  @EnumSource
  void testJsonToCsv_dataExtraction(DataqueryCsvExportService.SUPPORTED_LANGUAGES language) throws IOException {
    var dataExtraction = createValidDataExtraction();
    DisplayEntry displayEntry = createDisplayEntry();

    CodeableConceptEntry ccEntry = mock(CodeableConceptEntry.class);
    doReturn(displayEntry).when(ccEntry).display();
    doReturn(ccEntry).when(codeableConceptService).getSearchResultEntryByTermCode(any(TermCode.class));
    doReturn(Optional.of(createDseProfile())).when(dseProfileRepository).findByUrl(anyString());
    doReturn(createDseProfileApi()).when(objectMapper).readValue(anyString(), eq(de.numcodex.feasibility_gui_backend.dse.api.DseProfile.class));

    String csvResult = dataqueryCsvExportService.jsonToCsv(dataExtraction, language);

    assertNotNull(csvResult);
    assertTrue(csvResult.contains("basic testgroup"));
  }

  @Test
  void testAddFileToZip() throws IOException {
    String fileName = "dataquery.json";
    String fileContent = """
        {
            "foo": "bar",
            "baz": false,
            "foobar": 42
        }
        """;

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      dataqueryCsvExportService.addFileToZip(zos, fileName, fileContent);
    }

    try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
         ZipInputStream zis = new ZipInputStream(bais)) {

      ZipEntry entry = zis.getNextEntry();
      assertNotNull(entry, "zipfile is empty");
      assertEquals(fileName, entry.getName(), "wrong filename inside zip archive");

      byte[] buffer = new byte[1024];
      int len = zis.read(buffer);
      String extractedContent = new String(buffer, 0, len);
      assertEquals(fileContent, extractedContent, "zip content does not match");

      assertNull(zis.getNextEntry(), "too many entries in zipfile");
    }
  }

  private static Stream<Arguments> provideParamsForCriteriaTest() {
    List<Arguments> argumentsList = new ArrayList<>();
    for (DataqueryCsvExportService.SUPPORTED_LANGUAGES language : DataqueryCsvExportService.SUPPORTED_LANGUAGES.values()) {
      for (ValueFilterType valueFilterType : ValueFilterType.values()) {
        argumentsList.add(Arguments.of(language, valueFilterType, true, true, true));
        argumentsList.add(Arguments.of(language, valueFilterType, true, true, false));
        argumentsList.add(Arguments.of(language, valueFilterType, true, false, true));
        argumentsList.add(Arguments.of(language, valueFilterType, true, false, false));
        argumentsList.add(Arguments.of(language, valueFilterType, false, false, false));
        // When timerestriction is not included, there is no need to check different before/after date settings
      }
    }
    return Stream.of(argumentsList.toArray(Arguments[]::new));

  }

  private DataExtraction createValidDataExtraction() {
    return DataExtraction.builder()
        .attributeGroups(createAttributeGroups())
        .build();
  }

  private List<AttributeGroup> createAttributeGroups() {
    var referenceId = "my-referenced-group";
    return List.of(
        AttributeGroup.builder()
        .groupReference(URI.create("https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab"))
        .name("basic testgroup")
        .id("my-grp")
        .attributes(List.of(
            Attribute.builder().attributeRef("Observation.identifier").build(),
            Attribute.builder().attributeRef("Observation.status").build(),
            Attribute.builder().attributeRef("Observation.category").build(),
            Attribute.builder().attributeRef("Observation.code").build(),
            Attribute.builder().attributeRef("Observation.effective[x]").build()
        ))
        .build(),

        AttributeGroup.builder()
            .groupReference(URI.create("https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab"))
            .name("referencing testgroup")
            .id("my-referencing-grp")
            .attributes(List.of(
                Attribute.builder().attributeRef("Observation.identifier").build(),
                Attribute.builder().attributeRef("Observation.status").build(),
                Attribute.builder()
                    .attributeRef("Observation.category")
                    .linkedGroups(List.of(referenceId))
                    .build(),
                Attribute.builder().attributeRef("Observation.code").build(),
                Attribute.builder().attributeRef("Observation.effective[x]").build()
            ))
            .filter(
                List.of(
                    Filter.builder()
                        .type("date")
                        .name("date")
                        .start(LocalDate.of(2020, 1, 1))
                        .end(LocalDate.now())
                        .build()
                )
            )
            .build(),

        AttributeGroup.builder()
            .groupReference(URI.create("https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab"))
            .name("referenced testgroup")
            .id(referenceId)
            .includeReferenceOnly(true)
            .attributes(List.of(
                Attribute.builder().attributeRef("Observation.identifier").build(),
                Attribute.builder().attributeRef("Observation.status").build(),
                Attribute.builder().attributeRef("Observation.category").build(),
                Attribute.builder().attributeRef("Observation.code").build(),
                Attribute.builder().attributeRef("Observation.effective[x]").build()
            ))
            .filter(
                List.of(
                    Filter.builder()
                        .type("token")
                        .name("code")
                        .codes(List.of(
                            createTermCode()
                        ))
                        .build()
                )
            )
            .build()
        );
  }

  private StructuredQuery createValidStructuredQuery(ValueFilterType valueFilterType, boolean withTimerestriction, boolean withAfterDate, boolean withBeforeDate) {
    var termCode = TermCode.builder()
        .code("LL2191-6")
        .system("http://loinc.org")
        .display("Geschlecht")
        .build();
    var inclusionCriterion = Criterion.builder()
        .context(termCode)
        .termCodes(List.of(termCode))
        .attributeFilters(List.of(
            createAttributeFilter(ValueFilterType.CONCEPT),
            createAttributeFilter(ValueFilterType.REFERENCE),
            createAttributeFilter(ValueFilterType.QUANTITY_COMPARATOR)
        ))
        .valueFilter(createValueFilter(valueFilterType))
        .timeRestriction(withTimerestriction ? createTimeRestriction(withAfterDate, withBeforeDate) : null)
        .build();
    return StructuredQuery.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(inclusionCriterion)))
        .exclusionCriteria(null)
        .display("foo")
        .build();
  }

  private ValueFilter createValueFilter(ValueFilterType valueFilterType) {
    return switch (valueFilterType) {
      case CONCEPT -> ValueFilter.builder()
          .type(valueFilterType)
          .selectedConcepts(List.of(
              createTermCode()
          ))
          .build();
      case QUANTITY_COMPARATOR -> ValueFilter.builder()
          .type(valueFilterType)
          .comparator(Comparator.EQUAL)
          .value(42.0)
          .quantityUnit(Unit.builder()
              .code("a")
              .display("a")
              .build())
          .build();
      case QUANTITY_RANGE -> ValueFilter.builder()
          .type(valueFilterType)
          .minValue(18.0)
          .maxValue(42.0)
          .quantityUnit(Unit.builder()
              .code("a")
              .display("a")
              .build())
          .build();
      case REFERENCE -> ValueFilter.builder()
          .type(valueFilterType)
          .build();
    };
  }

  private AttributeFilter createAttributeFilter(ValueFilterType valueFilterType) {
    return switch (valueFilterType) {
      case CONCEPT -> AttributeFilter.builder()
          .type(valueFilterType)
          .attributeCode(createTermCode())
          .selectedConcepts(List.of(
              createTermCode()
          ))
          .build();
      case QUANTITY_COMPARATOR -> AttributeFilter.builder()
          .type(valueFilterType)
          .attributeCode(createTermCode())
          .comparator(Comparator.EQUAL)
          .value(42.0)
          .quantityUnit(Unit.builder()
              .code("a")
              .display("a")
              .build())
          .build();
      case QUANTITY_RANGE -> AttributeFilter.builder()
          .type(valueFilterType)
          .attributeCode(createTermCode())
          .minValue(18.0)
          .maxValue(42.0)
          .quantityUnit(Unit.builder()
              .code("a")
              .display("a")
              .build())
          .build();
      case REFERENCE -> AttributeFilter.builder()
          .type(valueFilterType)
          .attributeCode(createTermCode())
          .build();
    };
  }

  private DisplayEntry createDisplayEntry() {
    return DisplayEntry.builder()
        .original("original entry")
        .translations(List.of(
            LocalizedValue.builder().language("de-DE").value("deutscher Eintrag").build(),
            LocalizedValue.builder().language("en-US").value("English entry").build()
        ))
        .build();
  }

  private DseProfile createDseProfile() {
    var dseProfile = new DseProfile();
    dseProfile.setId(1L);
    dseProfile.setEntry("""
        {"display": {"original": "MII PR Diagnose Condition", "translations": [{"language": "de-DE", "value": "Diagnose"}, {"language": "en-US", "value": "Diagnosis"}]}, "module": {"original": "modul-diagnose", "translations": [{"language": "de-DE", "value": "Diagnose"}, {"language": "en-US", "value": "Diagnosis"}]}, "url": "https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose", "filters": [{"type": "date", "name": "recorded-date", "ui_type": "timeRestriction"}, {"type": "token", "name": "code", "ui_type": "code", "valueSetUrls": ["http://fhir.de/ValueSet/bfarm/icd-10-gm", "http://fhir.de/ValueSet/bfarm/alpha-id", "https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/ValueSet/diagnoses-sct", "https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/ValueSet/mii-vs-diagnose-orphanet"]}], "fields": [{"display": {"original": "code", "translations": [{"language": "de-DE", "value": "Code"}, {"language": "en-US", "value": "Code"}]}, "description": {"original": "Ein ICD-10-, Alpha-ID-, SNOMED-, Orpha- oder anderer Code, der die Diagnose identifiziert.", "translations": [{"language": "de-DE", "value": "Ein ICD-10-, Alpha-ID-, SNOMED-, Orpha- oder anderer Code, der die Diagnose identifiziert."}, {"language": "en-US", "value": "An ICD-10-, Alpha-ID-, SNOMED-, Orpha- or other code that identifies the diagnosis."}]}, "id": "Condition.code", "type": "CodeableConcept", "recommended": true, "required": false, "children": [{"display": {"original": "coding", "translations": [{"language": "de-DE", "value": ""}, {"language": "en-US", "value": ""}]}, "description": {"original": "A reference to a code defined by a terminology system.", "translations": [{"language": "de-DE", "value": ""}, {"language": "en-US", "value": ""}]}, "id": "Condition.code.coding", "type": "Coding", "recommended": false, "required": false, "children": [{"display": {"original": "sct", "translations": [{"language": "de-DE", "value": "SNOMED CT Code"}, {"language": "en-US", "value": "SNOMED CT code"}]}, "description": {"original": "Ein Verweis auf einen von SNOMED CT definierten Code", "translations": [{"language": "de-DE", "value": "Ein Verweis auf einen von SNOMED CT definierten Code"}, {"language": "en-US", "value": "A reference to a code defined by SNOMED CT"}]}, "id": "Condition.code.coding:sct", "type": "Coding", "recommended": false, "required": false, "children": []}, {"display": {"original": "icd10-gm", "translations": [{"language": "de-DE", "value": "ICD-10-GM Code"}, {"language": "en-US", "value": "ICD-10-GM code"}]}, "description": {"original": "Ein Verweis auf einen von der ICD-10-GM definierten Code", "translations": [{"language": "de-DE", "value": "Ein Verweis auf einen von der ICD-10-GM definierten Code"}, {"language": "en-US", "value": "A reference to a code defined by the ICD-10-GM"}]}, "id": "Condition.code.coding:icd10-gm", "type": "Coding", "recommended": false, "required": false, "children": []}, {"display": {"original": "alpha-id", "translations": [{"language": "de-DE", "value": "Alpha-ID Code"}, {"language": "en-US", "value": "Alpha-ID code"}]}, "description": {"original": "Ein Verweis auf einen von der Alpha-ID definierten Code", "translations": [{"language": "de-DE", "value": "Ein Verweis auf einen von der Alpha-ID definierten Code"}, {"language": "en-US", "value": "A reference to a code defined by the Alpha-ID"}]}, "id": "Condition.code.coding:alpha-id", "type": "Coding", "recommended": false, "required": false, "children": []}, {"display": {"original": "orphanet", "translations": [{"language": "de-DE", "value": "ORPHAcode"}, {"language": "en-US", "value": "ORPHAcode"}]}, "description": {"original": "Ein Verweis auf einen von der Orphanet Nomenklatur der Seltenen Krankheiten definierten Code", "translations": [{"language": "de-DE", "value": "Ein Verweis auf einen von der Orphanet Nomenklatur der Seltenen Krankheiten definierten Code"}, {"language": "en-US", "value": "A reference to a code defined by the Orphanet nomenclature of rare diseases"}]}, "id": "Condition.code.coding:orphanet", "type": "Coding", "recommended": false, "required": false, "children": []}]}]}, {"display": {"original": "note", "translations": [{"language": "de-DE", "value": "Hinweis"}, {"language": "en-US", "value": "Note"}]}, "description": {"original": "Zus\\u00e4tzliche Informationen zur Diagnose als Freitext.", "translations": [{"language": "de-DE", "value": "Zus\\u00e4tzliche Informationen zur Diagnose als Freitext."}, {"language": "en-US", "value": "Additional information about the diagnosis as free text."}]}, "id": "Condition.note", "type": "Annotation", "recommended": false, "required": false, "children": []}, {"display": {"original": "bodySite", "translations": [{"language": "de-DE", "value": "K\\u00f6rperstelle"}, {"language": "en-US", "value": "Body site"}]}, "description": {"original": "Die K\\u00f6rperstelle der Diagnose mittels SNOMED oder anderem Code.", "translations": [{"language": "de-DE", "value": "K\\u00f6rperstelle der Diagnose mittels SNOMED oder anderem Code."}, {"language": "en-US", "value": "The body site of the diagnosis using SNOMED or other systems."}]}, "id": "Condition.bodySite", "type": "CodeableConcept", "recommended": false, "required": false, "children": [{"display": {"original": "coding", "translations": [{"language": "de-DE", "value": ""}, {"language": "en-US", "value": ""}]}, "description": {"original": "A reference to a code defined by a terminology system.", "translations": [{"language": "de-DE", "value": ""}, {"language": "en-US", "value": ""}]}, "id": "Condition.bodySite.coding", "type": "Coding", "recommended": false, "required": false, "children": [{"display": {"original": "snomed-ct", "translations": [{"language": "de-DE", "value": "SNOMED CT Code"}, {"language": "en-US", "value": "SNOMED CT code"}]}, "description": {"original": "Ein Verweis auf einen von SNOMED CT definierten Code", "translations": [{"language": "de-DE", "value": "Ein Verweis auf einen von SNOMED CT definierten Code"}, {"language": "en-US", "value": "A reference to a code defined by SNOMED CT"}]}, "id": "Condition.bodySite.coding:snomed-ct", "type": "Coding", "recommended": false, "required": false, "children": []}]}]}, {"display": {"original": "onset", "translations": [{"language": "de-DE", "value": "Beginn"}, {"language": "en-US", "value": "Onset"}]}, "description": {"original": "Gesch\\u00e4tztes oder tats\\u00e4chliches Datum oder Zeitraum, an dem die Erkrankung begonnen hat, nach Meinung des Klinikers.", "translations": [{"language": "de-DE", "value": "Gesch\\u00e4tztes oder tats\\u00e4chliches Datum oder Zeitraum, an dem die Erkrankung begonnen hat, nach Meinung des Klinikers."}, {"language": "en-US", "value": "Estimated or actual date or date-time the condition began, in the opinion of the clinician."}]}, "id": "Condition.onset[x]", "type": "dateTime", "recommended": false, "required": false, "children": []}, {"display": {"original": "recordedDate", "translations": [{"language": "de-DE", "value": "Aufzeichnungsdatum"}, {"language": "en-US", "value": "Recorded date"}]}, "description": {"original": "Datum, an dem die Diagnose erstmals dokumentiert wurde.", "translations": [{"language": "de-DE", "value": "Datum, an dem die Diagnose erstmals dokumentiert wurde."}, {"language": "en-US", "value": "Date when the diagnosis was first recorded."}]}, "id": "Condition.recordedDate", "type": "dateTime", "recommended": true, "required": false, "children": []}, {"display": {"original": "clinicalStatus", "translations": [{"language": "de-DE", "value": "Klinischer Status"}, {"language": "en-US", "value": "Clinical status"}]}, "description": {"original": "aktiv | Rezidiv | R\\u00fcckfall | inaktiv | Remission | abgeklungen", "translations": [{"language": "de-DE", "value": "aktiv | Rezidiv | R\\u00fcckfall | inaktiv | Remission | abgeklungen"}, {"language": "en-US", "value": "active | recurrence | relapse | inactive | remission | resolved"}]}, "id": "Condition.clinicalStatus", "type": "CodeableConcept", "recommended": true, "required": false, "children": []}, {"display": {"original": "verificationStatus", "translations": [{"language": "de-DE", "value": "Verifizierungsstatus"}, {"language": "en-US", "value": "Verification status"}]}, "description": {"original": "unbest\\u00e4tigt | vorl\\u00e4ufig | differential | best\\u00e4tigt | widerlegt | fehlerhafte Eingabe", "translations": [{"language": "de-DE", "value": "unbest\\u00e4tigt | vorl\\u00e4ufig | differential | best\\u00e4tigt | widerlegt | fehlerhafte Eingabe"}, {"language": "en-US", "value": "unconfirmed | provisional | differential | confirmed | refuted | entered-in-error"}]}, "id": "Condition.verificationStatus", "type": "CodeableConcept", "recommended": true, "required": false, "children": []}, {"display": {"original": "Feststellungsdatum", "translations": [{"language": "de-DE", "value": "Feststellungsdatum"}, {"language": "en-US", "value": "Asserted date"}]}, "description": {"original": "Datum, an dem die Diagnose erstmals festgestellt wurde", "translations": [{"language": "de-DE", "value": "Datum, an dem die Diagnose erstmals festgestellt wurde"}, {"language": "en-US", "value": "Date the condition was first asserted"}]}, "id": "Condition.extension:Feststellungsdatum", "type": "Extension", "recommended": false, "required": false, "children": []}], "references": [{"display": {"original": "encounter", "translations": [{"language": "de-DE", "value": "Fall oder Kontakt"}, {"language": "en-US", "value": "Encounter"}]}, "description": {"original": "Fall oder Kontakt, bei dem die Diagnose festgestellt wurde.", "translations": [{"language": "de-DE", "value": "Fall oder Kontakt, bei dem die Diagnose festgestellt wurde."}, {"language": "en-US", "value": "Encounter during which the diagnosis was determined."}]}, "id": "Condition.encounter", "type": "Reference", "recommended": false, "required": false, "referencedProfiles": [{"url": "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/StructureDefinition/KontaktGesundheitseinrichtung", "display": {"original": "MII PR Fall Kontakt mit einer Gesundheitseinrichtung", "translations": [{"language": "de-DE", "value": "Fall - Kontakt mit einer Gesundheitseinrichtung"}, {"language": "en-US", "value": "Treatment case - Encounter with Health Care Facility"}]}, "fields": {"original": ["Aufnahmegrund", "ErsteUndZweiteStelle", "DritteStelle", "VierteStelle", "identifier", "Aufnahmenummer", "type", "vn-type", "status", "class", "type", "Kontaktebene", "KontaktArt", "serviceType", "coding", "Fachabteilungsschluessel", "ErweiterterFachabteilungsschluessel", "period", "diagnosis", "condition", "use", "coding", "Diagnosetyp", "DiagnosesubTyp", "hospitalization", "admitSource", "dischargeDisposition", "Entlassungsgrund", "partOf"], "translations": [{"language": "de-DE", "value": ["Aufnahmegrund", "1. und 2. Stelle", "3. Stelle", "4. Stelle", "Identifikator", "Aufnahmenummer", null, null, "Status", "Kontaktklasse", "Typ des Kontaktes", "Kontaktebene", "Kontaktart", "Fachabteilung", null, "Fachabteilungsschl\\u00fcssel", "Erweiterter Fachabteilungsschl\\u00fcssel", "Zeitraum des Kontaktes", "Diagnosen", null, null, null, null, null, "Klinikaufenthalt", null, null, null, "Teil von Kontakt"]}, {"language": "en-US", "value": ["Admission reason", "1st and 2nd position", "3rd position", "4th position", "Identifier", "Admission number", null, null, "status", "Classification of patient encounter", "Type of encounter", "Level of encounter", "Type of encounter", "Department", null, "Department key", "Extended department key", "Period of encounter", "Diagnoses", null, null, null, null, null, "Hospitalization", null, null, null, "Part of encounter"]}]}}], "children": []}, {"display": {"original": "ReferenzPrimaerdiagnose", "translations": [{"language": "de-DE", "value": ""}, {"language": "en-US", "value": ""}]}, "description": {"original": "This condition has an unspecified relationship with another condition.", "translations": [{"language": "de-DE", "value": ""}, {"language": "en-US", "value": ""}]}, "id": "Condition.extension:ReferenzPrimaerdiagnose", "type": "Reference", "recommended": false, "required": false, "referencedProfiles": [{"url": "https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/Todesursache", "display": {"original": "MII PR Person Todesursache", "translations": [{"language": "de-DE", "value": "Todesursache"}, {"language": "en-US", "value": "Cause of Death"}]}, "fields": {"original": ["clinicalStatus", "verificationStatus", "category", "todesDiagnose", "coding", "snomed", "loinc", "code", "coding", "icd10-who", "encounter", "recordedDate", "note"], "translations": [{"language": "de-DE", "value": ["Klinischer Status", "Verifizierungsstatus", "Kategorie", null, null, null, null, "Code", null, null, "Fall oder Kontakt", "Aufzeichnungsdatum", "Hinweis"]}, {"language": "en-US", "value": ["Clinical status", "Verification status", "Category", null, null, null, null, "Code", null, null, "Encounter", "Recorded date", "Note"]}]}}, {"url": "https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose", "display": {"original": "MII PR Diagnose Condition", "translations": [{"language": "de-DE", "value": "Diagnose"}, {"language": "en-US", "value": "Diagnosis"}]}, "fields": {"original": ["ReferenzPrimaerdiagnose", "Feststellungsdatum", "clinicalStatus", "verificationStatus", "code", "coding", "icd10-gm", "alpha-id", "sct", "orphanet", "bodySite", "coding", "snomed-ct", "encounter", "onset", "recordedDate", "note"], "translations": [{"language": "de-DE", "value": [null, "Feststellungsdatum", "Klinischer Status", "Verifizierungsstatus", "Code", null, "ICD-10-GM Code", "Alpha-ID Code", "SNOMED CT Code", "ORPHAcode", "K\\u00f6rperstelle", null, "SNOMED CT Code", "Fall oder Kontakt", "Beginn", "Aufzeichnungsdatum", "Hinweis"]}, {"language": "en-US", "value": [null, "Asserted date", "Clinical status", "Verification status", "Code", null, "ICD-10-GM code", "Alpha-ID code", "SNOMED CT code", "ORPHAcode", "Body site", null, "SNOMED CT code", "Encounter", "Onset", "Recorded date", "Note"]}]}}, {"url": "https://www.medizininformatik-initiative.de/fhir/ext/modul-onko/StructureDefinition/mii-pr-onko-diagnose-primaertumor", "display": {"original": "MII PR Onkologie Diagnose Prim\\u00e4rtumor", "translations": [{"language": "de-DE", "value": "Onkologische Diagnose im Rahmen einer onkologischen Erkrankung"}, {"language": "en-US"}]}, "fields": {"original": ["ReferenzPrimaerdiagnose", "Feststellungsdatum", "morphology-behavior-icdo3", "clinicalStatus", "verificationStatus", "condition-ver-status", "primaertumorDiagnosesicherung", "code", "coding", "icd10-gm", "alpha-id", "sct", "orphanet", "bodySite", "coding", "snomed-ct", "primaertumorSeitenlokalisation", "icd-o-3", "encounter", "onset", "recordedDate", "evidence", "detail", "note"], "translations": [{"language": "de-DE", "value": [null, "Feststellungsdatum", "ICD-O-Morphologie", "Klinischer Status", "Verifizierungsstatus", null, "Diagnosesicherung gem\\u00e4\\u00df oBDS", "Code", null, "ICD-10-GM Code", "Alpha-ID Code", "SNOMED CT Code", "ORPHAcode", "K\\u00f6rperstelle", null, "SNOMED CT Code", "Seitenlokalisation des Prim\\u00e4rtumors gem\\u00e4\\u00df oBDS", "ICD-O-Topographie", "Fall oder Kontakt", "Beginn", "Aufzeichnungsdatum", null, "Evidenz f\\u00fcr Erstdiagnose", "Hinweis"]}, {"language": "en-US", "value": [null, "Asserted date", null, "Clinical status", "Verification status", null, null, "Code", null, "ICD-10-GM code", "Alpha-ID code", "SNOMED CT code", "ORPHAcode", "Body site", null, "SNOMED CT code", null, null, "Encounter", "Onset", "Recorded date", null, null, "Note"]}]}}], "children": []}]}
        """);
    dseProfile.setUrl("https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose");
    return dseProfile;
  }

  private UiProfile createUiProfile() {
    var uiProfile = new UiProfile();
    uiProfile.setId(1L);
    uiProfile.setName("ui profile");
    uiProfile.setUiProfile("""
        {
            "attributeDefinitions": [],
            "name": "Diagnose",
            "timeRestrictionAllowed": true,
            "valueDefinition": null
        }
        """);
    return uiProfile;
  }

  private de.numcodex.feasibility_gui_backend.dse.api.DseProfile createDseProfileApi() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(createDseProfile().getEntry(), de.numcodex.feasibility_gui_backend.dse.api.DseProfile.class);
  }

  private de.numcodex.feasibility_gui_backend.terminology.api.UiProfile createUiProfileApi() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(createUiProfile().getUiProfile(), de.numcodex.feasibility_gui_backend.terminology.api.UiProfile.class);
  }

  private TimeRestriction createTimeRestriction(boolean includeAfterDate, boolean includeBeforeDate) {
    return TimeRestriction.builder()
        .afterDate(includeAfterDate ? "2025-04-16" : null)
        .beforeDate(includeBeforeDate ? "2025-04-17" : null)
        .build();
  }

  private TermCode createTermCode() {
    return TermCode.builder()
        .code("code")
        .system("system")
        .version("version")
        .display("display")
        .build();
  }
}
