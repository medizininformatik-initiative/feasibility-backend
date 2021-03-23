package de.numcodex.feasibility_gui_backend.service.query_builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.model.ui.TerminologyEntry;
import de.numcodex.feasibility_gui_backend.repository.QueryRepository;
import de.numcodex.feasibility_gui_backend.repository.ResultRepository;
import de.numcodex.feasibility_gui_backend.service.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import java.net.URI;
import java.net.URL;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;


class QueryBuilderFHIRTest {

  @Test
  public void queryBuilder() throws JsonProcessingException {

    RestTemplateBuilder builder = new RestTemplateBuilder();

    var fhirBuilder = new QueryBuilderFHIR(builder.build(), "http://localhost:5000");

    String testQuery = "{\n"
        + "    \"version\": \"http://to_be_decided.com/draft-1/schema#\",\n"
        + "    \"display\": \"\",\n"
        + "    \"inclusionCriteria\": [\n"
        + "      [\n"
        + "        {\n"
        + "          \"termCode\": {\n"
        + "            \"code\": \"29463-7\",\n"
        + "            \"system\": \"http://loinc.org\",\n"
        + "            \"version\": \"v1\",\n"
        + "            \"display\": \"Body Weight\"\n"
        + "        },\n"
        + "        \"valueFilter\": {\n"
        + "            \"type\": \"quantity-comparator\",\n"
        + "            \"unit\": {\n"
        + "              \"code\": \"kg\",\n"
        + "              \"display\": \"kilogram\"\n"
        + "            },\n"
        + "            \"comparator\": \"gt\",\n"
        + "            \"value\": 50\n"
        + "          }\n"
        + "        }\n"
        + "      ]\n"
        + "    ]\n"
        + "  }";


    var objectMapper = new ObjectMapper();
    var test = objectMapper.readValue(testQuery, StructuredQuery.class);
    var jsonString = objectMapper.writeValueAsString(test);
    System.out.println(jsonString);
    assert(! jsonString.contains("exclusionCriteria"));

    //assertEquals(testQueryForComp, jsonString);

  }

}