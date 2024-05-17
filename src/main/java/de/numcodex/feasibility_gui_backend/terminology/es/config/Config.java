package de.numcodex.feasibility_gui_backend.terminology.es.config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Configuration
@ConditionalOnExpression("${app.elasticSearchEnabled}")
public class Config extends ElasticsearchConfiguration {

  @Override
  public ClientConfiguration clientConfiguration() {
    return ClientConfiguration.builder()
        .connectedTo("localhost:9200")
        .build();
  }
}