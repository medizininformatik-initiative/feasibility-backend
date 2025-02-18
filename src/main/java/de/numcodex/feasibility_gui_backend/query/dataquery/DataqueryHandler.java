package de.numcodex.feasibility_gui_backend.query.dataquery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.Crtdl;
import de.numcodex.feasibility_gui_backend.query.api.Dataquery;
import de.numcodex.feasibility_gui_backend.query.api.status.SavedQuerySlots;
import de.numcodex.feasibility_gui_backend.query.persistence.DataqueryRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Transactional
@RequiredArgsConstructor
public class DataqueryHandler {

  @NonNull
  private ObjectMapper jsonUtil;

  @NonNull
  private DataqueryRepository dataqueryRepository;

  @NonNull
  private Integer maxDataqueriesPerUser;

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
}
