package de.numcodex.feasibility_gui_backend.service.query_builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;


class FhirQueryBuilderTest {

  @Test
  public void queryBuilder() throws JsonProcessingException {

    RestTemplateBuilder builder = new RestTemplateBuilder();

    var fhirBuilder = new FhirQueryBuilder(builder.build(), "http://localhost:5000");

    String testQuery = """
            {
                "version": "http://to_be_decided.com/draft-1/schema#",
                "display": "",
                "inclusionCriteria": [
                  [
                    {
                      "termCode": {
                        "code": "29463-7",
                        "system": "http://loinc.org",
                        "version": "v1",
                        "display": "Body Weight"
                    },
                    "valueFilter": {
                        "type": "quantity-comparator",
                        "unit": {
                          "code": "kg",
                          "display": "kilogram"
                        },
                        "comparator": "gt",
                        "value": 50
                      }
                    }
                  ]
                ]
              }""";


    var objectMapper = new ObjectMapper();
    var test = objectMapper.readValue(testQuery, StructuredQuery.class);
    var jsonString = objectMapper.writeValueAsString(test);
    System.out.println(jsonString);
    assert(! jsonString.contains("exclusionCriteria"));
    //assertEquals(testQueryForComp, jsonString);

  }

}
