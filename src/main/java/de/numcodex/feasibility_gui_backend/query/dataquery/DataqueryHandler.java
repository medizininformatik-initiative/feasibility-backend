package de.numcodex.feasibility_gui_backend.query.dataquery;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.numcodex.feasibility_gui_backend.query.api.Dataquery;
import de.numcodex.feasibility_gui_backend.query.api.status.SavedQuerySlots;
import de.numcodex.feasibility_gui_backend.query.persistence.DataqueryRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Transactional
@RequiredArgsConstructor
public class DataqueryHandler {

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
      de.numcodex.feasibility_gui_backend.query.persistence.Dataquery dataqueryEntity = de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.of(tmp);
      dataqueryEntity = dataqueryRepository.save(dataqueryEntity);
      return dataqueryEntity.getId();
    } catch (JsonProcessingException e) {
      throw new DataqueryException(e.getMessage());
    }
  }

  public Long storeExpiringDataquery(@NonNull Dataquery dataquery, @NonNull String userId, @NonNull String ttlDuration) throws DataqueryException, DataqueryStorageFullException {

    var tmp = Dataquery.builder()
        .resultSize(dataquery.resultSize())
        .content(dataquery.content())
        .label(dataquery.label())
        .comment(dataquery.comment())
        .createdBy(userId)
        .expiresAt(Timestamp.valueOf(LocalDateTime.now().plusSeconds(Duration.parse(ttlDuration).getSeconds())))
        .build();

    try {
      de.numcodex.feasibility_gui_backend.query.persistence.Dataquery dataqueryEntity = de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.of(tmp);
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
    return Dataquery.of(dataquery);
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
      var dataqueryToUpdate = de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.of(dataquery);
      dataqueryToUpdate.setId(existingDataquery.getId());
      dataqueryToUpdate.setCreatedBy(userId);
      dataqueryToUpdate.setLastModified(Timestamp.valueOf(LocalDateTime.now()));
      dataqueryRepository.save(dataqueryToUpdate);
    } else {
      throw new DataqueryException();
    }
  }

  public List<Dataquery> getDataqueriesByAuthor(String userId, boolean includeTemporary) throws DataqueryException {
    List<de.numcodex.feasibility_gui_backend.query.persistence.Dataquery> dataqueries;

    dataqueries = dataqueryRepository.findAllByCreatedBy(userId, includeTemporary);

    List<Dataquery> ret = new ArrayList<>();

    for (de.numcodex.feasibility_gui_backend.query.persistence.Dataquery dataquery : dataqueries) {
      try {
        ret.add(Dataquery.of(dataquery));
      } catch (JsonProcessingException e) {
        throw new DataqueryException();
      }
    }

    return ret;
  }

  public List<Dataquery> getDataqueriesByAuthor(String userId) throws DataqueryException {
    return getDataqueriesByAuthor(userId, false);
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
}
