package de.numcodex.feasibility_gui_backend.service.query_executor.impl.direct;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

public class DirectConnector {

  private final RestTemplate restTemplate;

  private String flareBaseUrl;


  public DirectConnector (RestTemplate restTemplate, @Value("${de.num-codex.FeasibilityGuiBackend.flare.baseUrl}") String flareBaseUrl){
    this.restTemplate = restTemplate;
    this.flareBaseUrl = flareBaseUrl;
  }

  
  public String getQueryResult(String query) {

    var queryTranslateEndpoint = "/query-sync";
    var url = flareBaseUrl + queryTranslateEndpoint;
    var headers = new HttpHeaders();
    headers.put("Content-Type", List.of("codex/json"));
    headers.put("Accept", List.of("internal/json"));

    HttpEntity<String> request = new HttpEntity<>(query, headers);

    var response = restTemplate.postForObject(url,request , String.class);
    return response;
  }

}
