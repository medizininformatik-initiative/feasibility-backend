package de.numcodex.feasibility_gui_backend.query.dataquery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import de.numcodex.feasibility_gui_backend.common.api.Comparator;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.dse.api.LocalizedValue;
import de.numcodex.feasibility_gui_backend.dse.persistence.DseProfile;
import de.numcodex.feasibility_gui_backend.dse.persistence.DseProfileRepository;
import de.numcodex.feasibility_gui_backend.query.api.*;
import de.numcodex.feasibility_gui_backend.terminology.api.AttributeDefinition;
import de.numcodex.feasibility_gui_backend.terminology.api.CodeableConceptEntry;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResultEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.CodeableConceptService;
import de.numcodex.feasibility_gui_backend.terminology.es.TerminologyEsService;
import de.numcodex.feasibility_gui_backend.terminology.persistence.UiProfile;
import de.numcodex.feasibility_gui_backend.terminology.persistence.UiProfileRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor
@Service
public class DataqueryCsvExportService {

  private static final String[] INCLUSION_HEADERS = {"module", "criterionDisplay", "criterionSystem", "criterionCode", "criterionFilter", "timeRestriction", "conjunctionGroup"};
  private static final String[] FEATURE_HEADERS = {"id", "module", "profileType", "featureName", "fields", "filter", "timeRestriction", "links", "onlyExtractIfLinked", "required"};
  private static final String FILTERTYPE_CODE = "code";
  private static final String FILTERTYPE_DATE = "date";
  private static final String FILTER_DELIMITER = "or";
  private static final String REFERENCING_GROUPS_DELIMITER = ", ";

  @Value("${app.export.csv.delimiter:;}")
  private char csvDelimiter;

  @Value("${app.export.csv.textwrapper:\"}")
  private char csvTextWrapper;

  private static final String filterCategorySeparator = " - ";
  private static final String filterEntrySeparator = ", ";

  @NonNull
  private ObjectMapper jsonUtil;

  @NonNull
  private final DseProfileRepository dseProfileRepository;

  @NonNull
  private final UiProfileRepository uiProfileRepository;

  @NonNull
  private final TerminologyEsService terminologyEsService;

  @NonNull
  private final CodeableConceptService codeableConceptService;

  @Builder
  record FieldsAndLinks(
      String fields,
      String links
  ) {
  }

  @Getter
  @RequiredArgsConstructor
  public enum SUPPORTED_LANGUAGES {
    DE("de-DE", "de"),
    EN("en-US", "en");
    private final String jsonKey;
    private final String language;
  };

  public String jsonToCsv(DataExtraction in, SUPPORTED_LANGUAGES lang) throws IOException {
    StringWriter stringWriter = new StringWriter();
    CSVWriter csvWriter = new CSVWriter(stringWriter,
        csvDelimiter,
        csvTextWrapper,
        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
        CSVWriter.DEFAULT_LINE_END);

    csvWriter.writeNext(
        Arrays.stream(FEATURE_HEADERS)
            .map(entry -> MultiMessageBundle.getEntry(entry, lang))
            .toArray(String[]::new)
    );

    for (AttributeGroup attributeGroup : in.attributeGroups()) {
      var dseProfileOptional = dseProfileRepository.findByUrl(attributeGroup.groupReference().toString());
      String[] row = getRow(attributeGroup, dseProfileOptional, in, lang);
      csvWriter.writeNext(row);
    }

    csvWriter.close();
    return stringWriter.toString();
  }

  public String jsonToCsv(List<List<Criterion>> in, SUPPORTED_LANGUAGES lang) throws IOException {
    StringWriter stringWriter = new StringWriter();
    CSVWriter csvWriter = new CSVWriter(stringWriter,
        csvDelimiter,
        csvTextWrapper,
        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
        CSVWriter.DEFAULT_LINE_END);

    csvWriter.writeNext(
        Arrays.stream(INCLUSION_HEADERS)
            .map(entry -> MultiMessageBundle.getEntry(entry, lang))
            .toArray(String[]::new)
    );

    for (ListIterator<List<Criterion>> outerIterator = in.listIterator(); outerIterator.hasNext(); ) {
      var groupIndex = outerIterator.nextIndex() + 1; // start with 1 instead of 0
      List<Criterion> criteriaList = outerIterator.next();

      for (Criterion criterion : criteriaList) {
        String[] row = getRow(criterion, groupIndex, lang);
        csvWriter.writeNext(row);
      }
    }

    csvWriter.close();
    return stringWriter.toString();
  }

  private String[] getRow(AttributeGroup attributeGroup, Optional<DseProfile> dseProfileOptional, DataExtraction dataExtraction, SUPPORTED_LANGUAGES lang) {
    String id = attributeGroup.id();
    String module = getModule(attributeGroup, lang);
    String profileType = getProfile(dseProfileOptional, lang);
    String featureName = attributeGroup.name();
    String filter = getFilter(attributeGroup, lang);
    String timeRestriction = getTimeRestriction(attributeGroup, lang);
    String onlyExtractIfLinked =
        (attributeGroup.includeReferenceOnly() != null && attributeGroup.includeReferenceOnly()) ? getReferencedBy(attributeGroup, dataExtraction, lang) : "";
    String isRequired = attributeGroup.attributes().stream().anyMatch(Attribute::mustHave) ? MultiMessageBundle.getEntry("yes", lang) : "";
    FieldsAndLinks fieldsAndLinks = getFieldsAndLinks(attributeGroup, dseProfileOptional, lang);

    return new String[]{id, module, profileType, featureName, fieldsAndLinks.fields(), filter, timeRestriction, fieldsAndLinks.links(), onlyExtractIfLinked, isRequired};
  }

  private FieldsAndLinks getFieldsAndLinks(AttributeGroup attributeGroup, Optional<DseProfile> dseProfileOptional, SUPPORTED_LANGUAGES lang) {
    List<String> fieldsList = new ArrayList<>();
    List<String> linksList = new ArrayList<>();

    for (Attribute attribute : attributeGroup.attributes()) {
      if (attribute.linkedGroups() != null && !attribute.linkedGroups().isEmpty()) {
        linksList.add(
            attribute.linkedGroups().stream()
                .map(lg -> MessageFormat.format("{0}{1}",
                    lg,
                    attribute.mustHave() ? " (" + MultiMessageBundle.getEntry("required", lang) + ")" : ""))
                .collect(Collectors.joining(" " + MultiMessageBundle.getEntry("and", lang) + " "))
        );
      } else if (dseProfileOptional.isPresent()) {
        try {
          var dseProfile = jsonUtil.readValue(dseProfileOptional.get().getEntry(), de.numcodex.feasibility_gui_backend.dse.api.DseProfile.class);
          var fieldEntry = dseProfile.fields().stream().filter(profile -> profile.id().equalsIgnoreCase(attribute.attributeRef())).findFirst().get();

          fieldsList.add(
              MessageFormat.format("{0}{1}",
                  getLocalizedDisplayEntry(fieldEntry.display(), lang),
                  attribute.mustHave() ? " (" + MultiMessageBundle.getEntry("required", lang) + ")" : "")
          );
        } catch (JsonProcessingException e) {
          // noop
        }
      } else {
        // will this ever happen?
      }
    }

    return FieldsAndLinks.builder()
        .fields(String.join(", ", fieldsList))
        .links(String.join(", ", linksList))
        .build();
  }

  private String getReferencedBy(AttributeGroup attributeGroup, DataExtraction dataExtraction, SUPPORTED_LANGUAGES lang) {
    if (attributeGroup.id() == null || attributeGroup.id().isEmpty()) {
      return "";
    }
    var referencingGroups = dataExtraction.attributeGroups().stream()
        .filter(ag -> ag.attributes().stream()
            .anyMatch(attribute -> attribute.linkedGroups().contains(attributeGroup.id())))
        .toList();
    return referencingGroups.stream()
        .map(ag -> MessageFormat.format(" [{0}: {1} ({2}) - {3}]",
            MultiMessageBundle.getEntry("criterion", lang),
            ag.name(),
            ag.id(),
            ag.attributes().stream()
                .filter(attribute -> attribute.linkedGroups().contains(attributeGroup.id()))
                .map(attr -> attr.attributeRef().split("\\.", 2)[1])
                .collect(Collectors.joining(", "))
            )
        )
        .collect(Collectors.joining(REFERENCING_GROUPS_DELIMITER));
  }

  private String getFilter(AttributeGroup attributeGroup, SUPPORTED_LANGUAGES lang) {
    var codeFilters = attributeGroup.filter().stream()
        .filter(filter -> filter.name().equalsIgnoreCase(FILTERTYPE_CODE))
        .toList();
    return codeFilters.stream()
        .flatMap(filter -> filter.codes().stream())
        .map(code -> {
          var display = terminologyEsService.findBestContextlessTermcodeDisplay(code);
          return MessageFormat.format("[{0} ({1}, {2})]", getLocalizedDisplayEntry(DisplayEntry.of(display), lang), code.code(), code.system());
        })
        .collect(Collectors.joining(" " + MultiMessageBundle.getEntry(FILTER_DELIMITER, lang) + " "));
  }

  private String getTimeRestriction(AttributeGroup attributeGroup, SUPPORTED_LANGUAGES lang) {
    var dateFilterOptional = attributeGroup.filter().stream()
        .filter(filter -> filter.type().equalsIgnoreCase(FILTERTYPE_DATE))
        .findFirst();
    if (dateFilterOptional.isPresent()) {
      var dateFilter = dateFilterOptional.get();
      String messagePattern = dateFilter.start() != null
          ? (dateFilter.end() != null ? "{0} < X < {1}" : "{0} < X")
          : (dateFilter.end() != null ? "X < {1}" : "");
      return MessageFormat.format(messagePattern,
          dateFilter.start(),
          dateFilter.end());
    }
    return "";
  }

  private String getProfile(Optional<DseProfile> dseProfileOptional, SUPPORTED_LANGUAGES lang) {
    if (dseProfileOptional.isPresent()) {
      try {
        var dseProfile = jsonUtil.readValue(dseProfileOptional.get().getEntry(), de.numcodex.feasibility_gui_backend.dse.api.DseProfile.class);
        return getLocalizedDisplayEntry(dseProfile.display(), lang);
      } catch (JsonProcessingException e) {
        return "";
      }
    } else {
      return "";
    }
  }

  private static String getModule(AttributeGroup attributeGroup, SUPPORTED_LANGUAGES lang) {
    return MultiMessageBundle.getEntry("tbdPlaceholder", lang);
  }

  private String[] getRow(Criterion criterion, int conjunctionGroup, SUPPORTED_LANGUAGES lang) {

    EsSearchResultEntry criterionEsEntry = terminologyEsService.getSearchResultEntryByCriterion(criterion);

    TermCode tc0 = criterion.termCodes().get(0);
    String contextDisplay = criterion.context().display();
    String termCodeDisplay = getLocalizedDisplayEntry(criterionEsEntry.display(), lang);
    String termCodeSystem = tc0.system();
    String termCodeCode = tc0.code();
    String filter = filterToString(criterion, lang);
    String timeRestriction = timeRestrictionToString(criterion.timeRestriction(), lang);

    return new String[]{contextDisplay, termCodeDisplay, termCodeSystem, termCodeCode, filter, timeRestriction, String.valueOf(conjunctionGroup)};
  }

  private String timeRestrictionToString(TimeRestriction timeRestriction, SUPPORTED_LANGUAGES lang) {
    if (timeRestriction == null) {
      return "";
    }
    String messagePattern = timeRestriction.afterDate() != null
        ? (timeRestriction.beforeDate() != null ? "{0} < X < {1}" : "{0} < X")
        : (timeRestriction.beforeDate() != null ? "X < {1}" : "");
    return MessageFormat.format(messagePattern,
        timeRestriction.afterDate(),
        timeRestriction.beforeDate());
  }

  private String filterToString(Criterion criterion, SUPPORTED_LANGUAGES lang) {
    var uiProfileOptional = uiProfileRepository
        .findByContextualizedTermcodeHash(TerminologyEsService.createContextualizedTermcodeHash(criterion));
    StringBuilder builder = new StringBuilder();
    if (criterion.valueFilter() != null) {
      builder.append("[");
      builder.append(valueFilterToString(criterion.valueFilter(), lang));
      builder.append("]");
    }
    if (criterion.attributeFilters() != null) {
      var attributeFilterList = new ArrayList<String>();
      criterion.attributeFilters().forEach(af -> attributeFilterList.add("[" + attributeFilterToString(af, uiProfileOptional, lang) + "]"));
      builder.append(String.join(filterCategorySeparator, attributeFilterList));
    }
    return builder.toString();
  }

  private String valueFilterToString(ValueFilter filter, SUPPORTED_LANGUAGES lang) {
    String unit = filter.quantityUnit() != null ? filter.quantityUnit().display() : "";
    switch (filter.type()) {
      case QUANTITY_COMPARATOR:
        return MessageFormat.format("{0} {1} {2}{3}",
            MultiMessageBundle.getEntry("value", lang),
            translateComparator(filter.comparator()),
            filter.value(),
            unit
        );
      case QUANTITY_RANGE:
        String messagePattern = filter.minValue() != null
            ? (filter.maxValue() != null ? "{2}{1} < {0} < {3}{1}" : "{2}{1} < {0}")
            : (filter.maxValue() != null ? "{0} < {3}{1}" : "");
        return MessageFormat.format(messagePattern,
            MultiMessageBundle.getEntry("value", lang),
            unit,
            filter.minValue(),
            filter.maxValue()
        );
      case CONCEPT:
        return MessageFormat.format("{0}: {1}",
            MultiMessageBundle.getEntry("value", lang),
            filter.selectedConcepts().stream()
              .map(tc -> {
                CodeableConceptEntry ccEntry = codeableConceptService.getSearchResultEntryByTermCode(tc);
                return ccEntry == null ? "" : getLocalizedDisplayEntry(ccEntry.display(), lang);
              })
              .collect(Collectors.joining(filterEntrySeparator)));
      case REFERENCE:
      default:
        return MessageFormat.format("{0}: {1}", MultiMessageBundle.getEntry("filtertypeUnimplemented", lang), filter.type());
    }
  }

  private String attributeFilterToString(AttributeFilter filter, Optional<UiProfile> uiProfileOptional, SUPPORTED_LANGUAGES lang) {
    String filterAcEntryString = getAttributeCodeTranslation(filter, uiProfileOptional, lang);
    switch (filter.type()) {
      case CONCEPT:
        return MessageFormat.format("{0}: {1}",
            filterAcEntryString,
            filter.selectedConcepts().stream()
                .map(tc -> {
                  CodeableConceptEntry ccEntry = codeableConceptService.getSearchResultEntryByTermCode(tc);
                  return ccEntry == null ? "" : getLocalizedDisplayEntry(ccEntry.display(), lang);
                })
                .collect(Collectors.joining(filterEntrySeparator)));
      case REFERENCE:
        return MessageFormat.format("{0}: {1}",
            filterAcEntryString,
            filter.criteria().stream()
                .flatMap(criterion -> criterion.termCodes().stream())
                .map(tc -> {
                  CodeableConceptEntry ccEntry = codeableConceptService.getSearchResultEntryByTermCode(tc);
                  return ccEntry == null ? "" : getLocalizedDisplayEntry(ccEntry.display(), lang);
                })
                .collect(Collectors.joining(filterEntrySeparator)));
      case QUANTITY_COMPARATOR:
      case QUANTITY_RANGE:
      default:
        return MessageFormat.format("{0}: {1}", MultiMessageBundle.getEntry("filtertypeUnimplemented", lang), filter.type());
    }
  }

  private String translateComparator(Comparator comparator) {
    if (comparator == null) {
      return "";
    }
    return switch (comparator) {
      case LESS_THAN -> "<";
      case GREATER_THAN -> ">";
      case GREATER_EQUAL -> ">=";
      case LESS_EQUAL -> "<=";
      case EQUAL -> "=";
      case UNEQUAL -> "!=";
    };
  }

  public void addFileToZip(ZipOutputStream zos, String fileName, String content) throws IOException {
    ZipEntry entry = new ZipEntry(fileName);
    zos.putNextEntry(entry);
    zos.write(content.getBytes());
    zos.closeEntry();
  }

  private String getLocalizedDisplayEntry(DisplayEntry displayEntry , SUPPORTED_LANGUAGES lang, boolean fallbackToOriginal) {
    Optional<LocalizedValue> localizedValueOptional = displayEntry.translations().stream()
        .filter(lv -> lv.language().equalsIgnoreCase(lang.getJsonKey()))
        .findFirst();

    if (localizedValueOptional.isPresent()) {
      var localizedValue = localizedValueOptional.get();
      var localizedEntry = localizedValue.value();
      if (localizedEntry != null && !localizedEntry.isBlank()) {
        return localizedEntry;
      }
    }
    return fallbackToOriginal ? displayEntry.original() : "";
  }

  private String getLocalizedDisplayEntry(DisplayEntry displayEntry , SUPPORTED_LANGUAGES lang) {
    return getLocalizedDisplayEntry(displayEntry, lang, true);
  }

  private String getAttributeCodeTranslation(AttributeFilter filter, Optional<UiProfile> uiProfileOptional, SUPPORTED_LANGUAGES lang) {
    String filterAcEntryString = "";
    if (uiProfileOptional.isPresent()) {
      var uiProfile = uiProfileOptional.get();
      try {
        de.numcodex.feasibility_gui_backend.terminology.api.UiProfile up = jsonUtil.readValue(uiProfile.getUiProfile(), de.numcodex.feasibility_gui_backend.terminology.api.UiProfile.class);
        Optional<AttributeDefinition> attributeDefinition = up.attributeDefinitions().stream()
            .filter(ad -> ad.attributeCode().equals(filter.attributeCode()))
            .findFirst();
        if (attributeDefinition.isPresent()) {
          filterAcEntryString = getLocalizedDisplayEntry(attributeDefinition.get().display(), lang);
        }
      } catch (JsonProcessingException e) {
        filterAcEntryString = "";
      }
    } else {
      filterAcEntryString = filter.attributeCode().display();
    }
    return filterAcEntryString;
  }
}
