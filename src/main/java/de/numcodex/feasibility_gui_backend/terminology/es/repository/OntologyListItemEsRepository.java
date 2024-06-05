package de.numcodex.feasibility_gui_backend.terminology.es.repository;

import de.numcodex.feasibility_gui_backend.terminology.es.model.OntologyListItemDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

@ConditionalOnExpression("${app.elastic.enabled}")
public interface OntologyListItemEsRepository extends ElasticsearchRepository<OntologyListItemDocument, String> {
  SearchHits<OntologyListItemDocument> findByNameContainingIgnoreCaseOrTermcodeContainingIgnoreCase(String name, String termcode);

  Page<OntologyListItemDocument> findByNameContainingIgnoreCaseOrTermcodeContainingIgnoreCase(String name, String termcode, Pageable pageable);

  @Query("{\"multi_match\":{\"query\":\"?0\",\"fields\":[ \"name\",\"termcode^2\"]}}")
  Page<OntologyListItemDocument> findByNameOrTermcodeMultiMatch(String searchterm, Pageable pageable);
}