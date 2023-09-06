package de.numcodex.feasibility_gui_backend.query.templates;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.QueryTemplate;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryHashCalculator;
import de.numcodex.feasibility_gui_backend.query.persistence.Query;
import de.numcodex.feasibility_gui_backend.query.persistence.QueryContent;
import de.numcodex.feasibility_gui_backend.query.persistence.QueryContentRepository;
import de.numcodex.feasibility_gui_backend.query.persistence.QueryRepository;
import de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplateRepository;
import java.sql.Timestamp;
import java.time.Instant;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

@Slf4j
@Transactional
@RequiredArgsConstructor
public class QueryTemplateHandler {

  @NonNull
  private QueryHashCalculator queryHashCalculator;

  @NonNull
  private ObjectMapper jsonUtil;

  @NonNull
  private QueryRepository queryRepository;

  @NonNull
  private QueryContentRepository queryContentRepository;

  @NonNull
  private QueryTemplateRepository queryTemplateRepository;

  public Long storeTemplate(QueryTemplate queryTemplateApi, String userId)
      throws QueryTemplateException {

    if (queryTemplateRepository.existsQueryTemplateByLabelAndUserId(queryTemplateApi.label(), userId)) {
      throw new DataIntegrityViolationException(String.format("User %s already has a query template named %s", userId, queryTemplateApi.label()));
    }

    Long queryId = storeNewQuery(queryTemplateApi.content(), userId);
    de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate queryTemplate
        = convertApiToPersistence(queryTemplateApi, queryId);
    queryTemplate = queryTemplateRepository.save(queryTemplate);
    return queryTemplate.getId();
  }

  public Long storeNewQuery(StructuredQuery query, String userId) throws QueryTemplateException {
    var querySerialized = serializedStructuredQuery(query);

    var queryHash = queryHashCalculator.calculateSerializedQueryBodyHash(querySerialized);
    var queryBody = queryContentRepository.findByHash(queryHash)
        .orElseGet(() -> {
          var freshQueryBody = new QueryContent(querySerialized);
          freshQueryBody.setHash(queryHash);
          return queryContentRepository.save(freshQueryBody);
        });

    var queryId = persistQuery(queryBody, userId);
    log.info("enqueued query '%s'".formatted(queryId));
    return queryId;
  }

  private Long persistQuery(QueryContent queryBody, String userId) {
    var feasibilityQuery = new Query();
    feasibilityQuery.setCreatedAt(Timestamp.from(Instant.now()));
    feasibilityQuery.setCreatedBy(userId);
    feasibilityQuery.setQueryContent(queryBody);
    return queryRepository.save(feasibilityQuery).getId();
  }

  private String serializedStructuredQuery(StructuredQuery query) throws QueryTemplateException {
    if (query == null) {
      throw new QueryTemplateException("Submitted query is null");
    }
    try {
      return jsonUtil.writeValueAsString(query);
    } catch (JsonProcessingException e) {
      throw new QueryTemplateException("could not serialize query in order to save it for a template", e);
    }
  }

  public de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate convertApiToPersistence(
      QueryTemplate in, Long queryId) {
    de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate out = new de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate();

    out.setQuery(queryRepository.getReferenceById(queryId));
    out.setComment(in.comment());
    out.setLabel(in.label());
    if (in.lastModified() != null) {
      out.setLastModified(Timestamp.valueOf(in.lastModified()));
    }
    return out;
  }

  public QueryTemplate convertPersistenceToApi(
      de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate in)
      throws JsonProcessingException {

    ObjectMapper jsonUtil = new ObjectMapper();
    return QueryTemplate.builder()
            .id(in.getId())
            .content(jsonUtil.readValue(in.getQuery().getQueryContent().getQueryContent(), StructuredQuery.class))
            .label(in.getLabel())
            .comment(in.getComment())
            .lastModified(in.getLastModified().toString())
            .createdBy(in.getQuery().getCreatedBy())
            .build();
  }
}
