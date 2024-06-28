package de.numcodex.feasibility_gui_backend.terminology.es.repository;

import de.numcodex.feasibility_gui_backend.terminology.es.model.OntologyListItemDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

@ConditionalOnExpression("${app.elastic.enabled}")
public interface OntologyListItemEsRepository extends ElasticsearchRepository<OntologyListItemDocument, String> {

  @Query("""
      {
        "bool": {
          "must": [
            {
              "multi_match": {
                "query": "?0",
                "fields": [
                  "name",
                  "termcode^2"
                ]
              }
            }
          ]
        }
      }
      """
  )
  Page<OntologyListItemDocument> findByNameOrTermcodeMultiMatch0Filters(String searchterm,
                                                                Pageable pageable);

  @Query("""
      {
        "bool": {
          "must": [
            {
              "multi_match": {
                "query": "?0",
                "fields": [
                  "name",
                  "termcode^2"
                ]
              }
            }
          ],
          "filter": {
            "bool" : {
              "must" : [
                {"range" : { "availability" : { "gt" : 0 } } }
              ]
            }
          }
        }
      }
      """
  )
  Page<OntologyListItemDocument> findByNameOrTermcodeMultiMatch0FiltersAvailableOnly(String searchterm,
                                                                        Pageable pageable);

  @Query("""
      {
        "bool": {
          "must": [
            {
              "multi_match": {
                "query": "?0",
                "fields": [
                  "name",
                  "termcode^2"
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
  Page<OntologyListItemDocument> findByNameOrTermcodeMultiMatch1Filter(String searchterm,
                                                                        String filterKey,
                                                                        List<String> filterValues,
                                                                        Pageable pageable);

  @Query("""
      {
        "bool": {
          "must": [
            {
              "multi_match": {
                "query": "?0",
                "fields": [
                  "name",
                  "termcode^2"
                ]
              }
            }
          ],
          "filter": {
            "bool" : {
              "must" : [
                {"terms" : { "?1": ?2 } },
                {"range" : { "availability" : { "gt" : 0 } } }
              ]
            }
          }
        }
      }
      """
  )
  Page<OntologyListItemDocument> findByNameOrTermcodeMultiMatch1FilterAvailableOnly(String searchterm,
                                                                       String filterKey,
                                                                       List<String> filterValues,
                                                                       Pageable pageable);

  @Query("""
      {
        "bool": {
          "must": [
            {
              "multi_match": {
                "query": "?0",
                "fields": [
                  "name",
                  "termcode^2"
                ]
              }
            }
          ],
          "filter": {
            "bool" : {
              "must" : [
                {"terms" : { "?1": ?2 } },
                {"terms" : { "?3": ?4 } }
              ]
            }
          }
        }
      }
      """
  )
  Page<OntologyListItemDocument> findByNameOrTermcodeMultiMatch2Filters(String searchterm,
                                                                        String filterKey1,
                                                                        List<String> filterValues1,
                                                                        String filterKey2,
                                                                        List<String> filterValues2,
                                                                        Pageable pageable);

  @Query("""
      {
        "bool": {
          "must": [
            {
              "multi_match": {
                "query": "?0",
                "fields": [
                  "name",
                  "termcode^2"
                ]
              }
            }
          ],
          "filter": {
            "bool" : {
              "must" : [
                {"terms" : { "?1": ?2 } },
                {"terms" : { "?3": ?4 } },
                {"range" : { "availability" : { "gt" : 0 } } }
              ]
            }
          }
        }
      }
      """
  )
  Page<OntologyListItemDocument> findByNameOrTermcodeMultiMatch2FiltersAvailableOnly(String searchterm,
                                                                        String filterKey1,
                                                                        List<String> filterValues1,
                                                                        String filterKey2,
                                                                        List<String> filterValues2,
                                                                        Pageable pageable);

  @Query("""
      {
        "bool": {
          "must": [
            {
              "multi_match": {
                "query": "?0",
                "fields": [
                  "name",
                  "termcode^2"
                ]
              }
            }
          ],
          "filter": {
            "bool" : {
              "must" : [
                {"terms" : { "?1": ?2 } },
                {"terms" : { "?3": ?4 } },
                {"terms" : { "?5": ?6 } }
              ]
            }
          }
        }
      }
      """
  )
  Page<OntologyListItemDocument> findByNameOrTermcodeMultiMatch3Filters(String searchterm,
                                                                        String filterKey1,
                                                                        List<String> filterValues1,
                                                                        String filterKey2,
                                                                        List<String> filterValues2,
                                                                        String filterKey3,
                                                                        List<String> filterValues3,
                                                                        Pageable pageable);

  @Query("""
        {
          "bool": {
            "must": [
              {
                "multi_match": {
                  "query": "?0",
                  "fields": [
                    "name",
                    "termcode^2"
                  ]
                }
              }
            ],
            "filter": {
              "bool" : {
                "must" : [
                  {"terms" : { "?1": ?2 } },
                  {"terms" : { "?3": ?4 } },
                  {"terms" : { "?5": ?6 } },
                  {"range" : { "availability" : { "gt" : 0 } } }
                ]
              }
            }
          }
        }
        """
  )
  Page<OntologyListItemDocument> findByNameOrTermcodeMultiMatch3FiltersAvailableOnly(String searchterm,
                                                                        String filterKey1,
                                                                        List<String> filterValues1,
                                                                        String filterKey2,
                                                                        List<String> filterValues2,
                                                                        String filterKey3,
                                                                        List<String> filterValues3,
                                                                        Pageable pageable);
}
