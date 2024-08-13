package de.numcodex.feasibility_gui_backend.terminology.es.repository;

import de.numcodex.feasibility_gui_backend.terminology.es.model.CodableConceptDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

@ConditionalOnExpression("${app.elastic.enabled}")
public interface CodableConceptEsRepository extends ElasticsearchRepository<CodableConceptDocument, String> {
  @Query("""
      {
        "bool": {
          "must": [
            {
              "multi_match": {
                "query": "?0",
                "fields": [
                  "termcode.display",
                  "termcode.code^2"
                ]
              }
            }
          ]
        }
      }
      """
  )
  Page<CodableConceptDocument> findByNameOrTermcodeMultiMatch0Filters(String searchterm,
                                                                        Pageable pageable);

  @Query("""
      {
        "bool": {
          "must": [
            {
              "multi_match": {
                "query": "?0",
                "fields": [
                  "termcode.display",
                  "termcode.code^2"
                ]
              }
            }
          ],
          "filter": {
            "bool" : {
              "must" : [
                {"terms" : { "?1": ?2 } }
              ]
            }
          }
        }
      }
      """
  )
  Page<CodableConceptDocument> findByNameOrTermcodeMultiMatch1Filter(String searchterm,
                                                                       String filterKey,
                                                                       List<String> filterValues,
                                                                       Pageable pageable);
}