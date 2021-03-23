package de.numcodex.feasibility_gui_backend.service.query_builder;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

public class FhirQueryBuilder implements QueryBuilder {

  private final RestTemplate restTemplate;
  private final String flareBaseUrl;

  public FhirQueryBuilder(RestTemplate restTemplate, String flareBaseUrl){
    this.restTemplate = Objects.requireNonNull(restTemplate);
    this.flareBaseUrl = Objects.requireNonNull(flareBaseUrl);
  }

  @Override
  public String getQueryContent(StructuredQuery query) {

    var queryTranslateEndpoint = "/query-translate";
    var url = flareBaseUrl + queryTranslateEndpoint;

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(Include.NON_NULL);

    var headers = new HttpHeaders();

    headers.put("Content-Type", List.of("codex/json"));
    headers.put("Accept", List.of("internal/json"));

    HttpEntity<String> request = null;
    try {
      request = new HttpEntity<>(objectMapper.writeValueAsString(query), headers);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }

    var response = restTemplate.postForObject(url,request , String.class);
    return response;
  }
}
