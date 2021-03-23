package de.numcodex.feasibility_gui_backend.service.query_builder;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class QueryBuilderFHIR implements QueryBuilder {

  private final RestTemplate restTemplate;

  private String flareBaseUrl;

  public QueryBuilderFHIR (RestTemplate restTemplate, @Value("${de.num-codex.FeasibilityGuiBackend.flare.baseUrl}") String flareBaseUrl){
    this.restTemplate = restTemplate;
    this.flareBaseUrl = flareBaseUrl;
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
