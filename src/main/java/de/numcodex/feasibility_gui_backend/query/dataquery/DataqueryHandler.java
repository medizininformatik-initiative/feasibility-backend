package de.numcodex.feasibility_gui_backend.query.dataquery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import de.numcodex.feasibility_gui_backend.common.api.Comparator;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.*;
import de.numcodex.feasibility_gui_backend.query.api.status.SavedQuerySlots;
import de.numcodex.feasibility_gui_backend.query.persistence.DataqueryRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.Principal;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Transactional
@RequiredArgsConstructor
public class DataqueryHandler {

  public static final String[] INCLUSION_EXCLUSION_HEADERS = {"Module", "Criterion display", "Criterion system", "Criterion code", "Criterion Filter", "Time Restriction", "Conjunction"};
  @NonNull
  private ObjectMapper jsonUtil;

  @NonNull
  private DataqueryRepository dataqueryRepository;

  @NonNull
  private Integer maxDataqueriesPerUser;

  @Value("${app.export.csv.delimiter:;}")
  private char csvDelimiter;

  @Value("${app.export.csv.textwrapper:\"}")
  private char csvTextWrapper;

  private static String filterCategorySeparator = " - ";
  private static String filterEntrySeparator = ", ";

  public Long storeDataquery(@NonNull Dataquery dataquery, @NonNull String userId) throws DataqueryException, DataqueryStorageFullException {

    // By definition, a user can save an unlimited amount of queries without result
    if (dataquery.resultSize() != null && dataqueryRepository.countByCreatedByWhereResultIsNotNull(userId) >= maxDataqueriesPerUser) {
      throw new DataqueryStorageFullException();
    }

    var tmp = Dataquery.builder()
        .resultSize(dataquery.resultSize())
        .content(dataquery.content())
        .label(dataquery.label())
        .comment(dataquery.comment())
        .createdBy(userId)
        .build();

    try {
      de.numcodex.feasibility_gui_backend.query.persistence.Dataquery dataqueryEntity = convertApiToPersistence(tmp);
      dataqueryEntity = dataqueryRepository.save(dataqueryEntity);
      return dataqueryEntity.getId();
    } catch (JsonProcessingException e) {
      throw new DataqueryException(e.getMessage());
    }
  }

  public Dataquery getDataqueryById(Long dataqueryId, String userId) throws DataqueryException, JsonProcessingException {
    de.numcodex.feasibility_gui_backend.query.persistence.Dataquery dataquery = dataqueryRepository.findById(dataqueryId).orElseThrow(DataqueryException::new);
    if (dataquery.getCreatedBy() == null || !dataquery.getCreatedBy().equals(userId)) {
      throw new DataqueryException();
    }
    return convertPersistenceToApi(dataquery);
  }

  public void updateDataquery(Long queryId, Dataquery dataquery, String userId) throws DataqueryException,  DataqueryStorageFullException, JsonProcessingException {
    var usedSlots = dataqueryRepository.countByCreatedByWhereResultIsNotNull(userId);
    var existingDataquery = dataqueryRepository.findById(queryId).orElseThrow(DataqueryException::new);

    if (usedSlots >= maxDataqueriesPerUser) {
      // Only throw an exception when the updated query contains a result and the original didn't
      if (dataquery.resultSize() != null && existingDataquery.getResultSize() == null) {
        throw new DataqueryStorageFullException();
      }
    }

    if (existingDataquery.getCreatedBy().equals(userId)) {
      var dataqueryToUpdate = convertApiToPersistence(dataquery);
      dataqueryToUpdate.setId(existingDataquery.getId());
      dataqueryToUpdate.setCreatedBy(userId);
      dataqueryToUpdate.setLastModified(Timestamp.valueOf(LocalDateTime.now()));
      dataqueryRepository.save(dataqueryToUpdate);
    } else {
      throw new DataqueryException();
    }
  }

  public List<Dataquery> getDataqueriesByAuthor(String userId) throws DataqueryException {
    List<de.numcodex.feasibility_gui_backend.query.persistence.Dataquery> dataqueries = dataqueryRepository.findAllByCreatedBy(userId);
    List<Dataquery> ret = new ArrayList<>();

    for (de.numcodex.feasibility_gui_backend.query.persistence.Dataquery dataquery : dataqueries) {
      try {
        ret.add(convertPersistenceToApi(dataquery));
      } catch (JsonProcessingException e) {
        throw new DataqueryException();
      }
    }

    return ret;
  }

  public void deleteDataquery(Long dataqueryId, String userId) throws DataqueryException {
    de.numcodex.feasibility_gui_backend.query.persistence.Dataquery dataquery = dataqueryRepository.findById(dataqueryId).orElseThrow(DataqueryException::new);
    if (!dataquery.getCreatedBy().equals(userId)) {
      throw new DataqueryException();
    } else {
      dataqueryRepository.delete(dataquery);
    }
  }

  public SavedQuerySlots getDataquerySlotsJson(String userId) {
    var queryAmount = dataqueryRepository.countByCreatedByWhereResultIsNotNull(userId);

    return SavedQuerySlots.builder()
        .used(queryAmount)
        .total(maxDataqueriesPerUser)
        .build();
  }

  public de.numcodex.feasibility_gui_backend.query.persistence.Dataquery convertApiToPersistence(de.numcodex.feasibility_gui_backend.query.api.Dataquery in) throws JsonProcessingException {
    de.numcodex.feasibility_gui_backend.query.persistence.Dataquery out = new de.numcodex.feasibility_gui_backend.query.persistence.Dataquery();
    out.setId(in.id() > 0 ? in.id() : null);
    out.setLabel(in.label());
    out.setComment(in.comment());
    if (in.lastModified() != null) {
      out.setLastModified(Timestamp.valueOf(in.lastModified()));
    }
    out.setCreatedBy(in.createdBy());
    out.setResultSize(in.resultSize());
    out.setCrtdl(jsonUtil.writeValueAsString(in.content()));
    return out;
  }

  public de.numcodex.feasibility_gui_backend.query.api.Dataquery convertPersistenceToApi(de.numcodex.feasibility_gui_backend.query.persistence.Dataquery in) throws JsonProcessingException {
    return de.numcodex.feasibility_gui_backend.query.api.Dataquery.builder()
        .id(in.getId())
        .label(in.getLabel())
        .comment(in.getComment())
        .createdBy(in.getCreatedBy())
        .resultSize(in.getResultSize())
        .lastModified(in.getLastModified() == null ? null : in.getLastModified().toString())
        .content(jsonUtil.readValue(in.getCrtdl(), Crtdl.class))
        .build();
  }

  public ByteArrayOutputStream createCsvExportZipfile(Long dataqueryId, Principal principal) throws DataqueryException, IOException {
    var dataquery = getDataqueryById(dataqueryId, principal.getName());
    if (dataquery.content() == null || dataquery.content().cohortDefinition() == null) {
      throw new DataqueryException("No ccdl part present");
    }
    var byteArrayOutputStream = new ByteArrayOutputStream();
    var zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
    Map<String, String> files = new HashMap<>();
    files.put(dataquery.label() + "_crtdl.json", jsonUtil.writeValueAsString(dataquery.content()));
    if (dataquery.content().cohortDefinition().inclusionCriteria() == null) {
      // Call with empty lists to just write the headers to the file
      files.put(dataquery.label() + "_inclusion.csv", jsonToCsv(List.of(List.of())));
    } else {
      files.put(dataquery.label() + "_inclusion.csv", jsonToCsv(dataquery.content().cohortDefinition().inclusionCriteria()));
    }
    if (dataquery.content().cohortDefinition().exclusionCriteria() == null) {
      // Call with empty lists to just write the headers to the file
      files.put(dataquery.label() + "_exclusion.csv", jsonToCsv(List.of(List.of())));
    } else {
      files.put(dataquery.label() + "_exclusion.csv", jsonToCsv(dataquery.content().cohortDefinition().exclusionCriteria()));
    }
    files.put(dataquery.label() + "_features.csv", "foo");

    for (Map.Entry<String, String> file : files.entrySet()) {
      addFileToZip(zipOutputStream, file.getKey(), file.getValue());
    }

    zipOutputStream.close();
    byteArrayOutputStream.close();
    return byteArrayOutputStream;
  }

  private String jsonToCsv(List<List<Criterion>> in) throws IOException {
    StringWriter stringWriter = new StringWriter();
    CSVWriter csvWriter = new CSVWriter(stringWriter,
        csvDelimiter,
        csvTextWrapper,
        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
        CSVWriter.DEFAULT_LINE_END);
    csvWriter.writeNext(INCLUSION_EXCLUSION_HEADERS);

    for (Iterator<List<Criterion>> outerIterator = in.iterator(); outerIterator.hasNext(); ) {
      List<Criterion> criteriaList = outerIterator.next();

      for (Iterator<Criterion> innerIterator = criteriaList.iterator(); innerIterator.hasNext(); ) {
        Criterion criterion = innerIterator.next();
        String[] row = getRow(criterion, getConjunction(innerIterator.hasNext(), outerIterator.hasNext()));
        csvWriter.writeNext(row);
      }
    }

    csvWriter.close();
    return stringWriter.toString();
  }

  private static String getConjunction(boolean innerIteratorHasNext, boolean outerIteratorHasNext) {
    return innerIteratorHasNext ? "OR" : (outerIteratorHasNext ? "AND" : "");
  }

  private static String[] getRow(Criterion criterion, String conjunction) {
    TermCode tc0 = criterion.termCodes().get(0);
    String contextDisplay = criterion.context().display();
    String termCodeDisplay = tc0.display();
    String termCodeSystem = tc0.system();
    String termCodeCode = tc0.code();
    String filter = filterToString(criterion.valueFilter(), criterion.attributeFilters());
    String timeRestriction = timeRestrictionToString(criterion.timeRestriction());

    return new String[]{contextDisplay, termCodeDisplay, termCodeSystem, termCodeCode, filter, timeRestriction, conjunction};
  }

  private static String timeRestrictionToString(TimeRestriction timeRestriction) {
    if (timeRestriction == null) {
      return "";
    }
    return MessageFormat.format("{0} < X < {1}",
        timeRestriction.afterDate() == null ? "" : timeRestriction.afterDate(),
        timeRestriction.beforeDate() == null ? "" : timeRestriction.beforeDate());
  }

  private static String filterToString(ValueFilter valueFilter, List<AttributeFilter> attributeFilters) {
    StringBuilder builder = new StringBuilder();
    if (valueFilter != null) {
      builder.append(valueFilterToString(valueFilter));
    }
    if (attributeFilters != null) {
      var attributeFilterList = new ArrayList<String>();
      attributeFilters.forEach(af -> attributeFilterList.add(attributeFilterToString(af)));
      builder.append(attributeFilterList.stream().collect(Collectors.joining(filterCategorySeparator)));
    }
    return builder.toString();
  }

  private static String valueFilterToString(ValueFilter filter) {
    String unit = filter.quantityUnit() != null ? filter.quantityUnit().display() : "";
    switch (filter.type()) {
      case QUANTITY_COMPARATOR:
        return MessageFormat.format("Value {0} {1}{2}",
            translateComparator(filter.comparator()),
            filter.value(),
            unit
        );
      case QUANTITY_RANGE:
        return MessageFormat.format("{1}{0} < X < {2}{0}",
            unit,
            filter.minValue(),
            filter.maxValue()
            );
      case CONCEPT:
        return MessageFormat.format("Value {0}", filter.selectedConcepts().stream()
            .map(TermCode::display)
            .collect(Collectors.joining(filterEntrySeparator)));
      case REFERENCE:
      default:
        return MessageFormat.format("Filterytpe {0} currently not implemented", filter.type());
    }
  }

  private static String attributeFilterToString(AttributeFilter filter) {
    switch (filter.type()) {
      case CONCEPT:
        return MessageFormat.format("{0}: {1}",
            filter.attributeCode().display(),
            filter.selectedConcepts().stream()
              .map(TermCode::display)
              .collect(Collectors.joining(filterEntrySeparator)));
      case REFERENCE:
        return MessageFormat.format("{0}: {1}",
            filter.attributeCode().display(),
            filter.criteria().stream()
                .flatMap(criterion -> criterion.termCodes().stream())
                .map(TermCode::display)
                .collect(Collectors.joining(filterEntrySeparator)));
      case QUANTITY_COMPARATOR:
      case QUANTITY_RANGE:
      default:
        return MessageFormat.format("Filterytpe {0} currently not implemented", filter.type());
    }
  }

  private static String translateComparator(Comparator comparator) {
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

  private void addFileToZip(ZipOutputStream zos, String fileName, String content) throws IOException {
    ZipEntry entry = new ZipEntry(fileName);
    zos.putNextEntry(entry);
    zos.write(content.getBytes());
    zos.closeEntry();
  }

}
