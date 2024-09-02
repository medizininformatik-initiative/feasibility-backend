package de.numcodex.feasibility_gui_backend.terminology.es.repository;

import de.numcodex.feasibility_gui_backend.terminology.es.model.CodeableConceptDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

@ConditionalOnExpression("${app.elastic.enabled}")
public interface CodeableConceptEsRepository extends ElasticsearchRepository<CodeableConceptDocument, String> {
}