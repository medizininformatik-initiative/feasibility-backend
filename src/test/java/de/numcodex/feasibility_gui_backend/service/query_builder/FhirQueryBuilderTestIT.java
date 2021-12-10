package de.numcodex.feasibility_gui_backend.service.query_builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;


class FhirQueryBuilderTestIT {

  // TODO: re-enable when implementing integration + system tests (CI pipelines external projects are required for docker images)
  @Disabled
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
                      "termCodes": [{
                        "code": "29463-7",
                        "system": "http://loinc.org",
                        "version": "v1",
                        "display": "Body Weight"
                    }],
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
    var structQuery = objectMapper.readValue(testQuery, StructuredQuery.class);
    var structQueryAnswer = fhirBuilder.getQueryContent(structQuery);
    System.out.println(structQueryAnswer);

  }

}
