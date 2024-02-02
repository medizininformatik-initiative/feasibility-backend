package de.numcodex.feasibility_gui_backend.terminology.es.repository;

import de.numcodex.feasibility_gui_backend.terminology.es.model.OntologyItemDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface OntologyItemEsRepository extends ElasticsearchRepository<OntologyItemDocument, String> {}