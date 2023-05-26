package de.numcodex.feasibility_gui_backend.query.api.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.QueryTemplate;
import java.io.InputStream;
import jakarta.validation.ConstraintValidator;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class QueryTemplateValidatorSpringConfig {

  private static final String JSON_SCHEMA = "query-template-schema.json";

  @Value("${app.enableQueryValidation}")
  private boolean enabled;

  @Bean
  public ConstraintValidator<QueryTemplateValidation, QueryTemplate> createStoredQueryValidator(
          @Qualifier("validation-template") Schema schema) {
    return enabled
            ? new QueryTemplateValidator(schema, new ObjectMapper())
            : new QueryTemplatePassValidator();
  }

  @Qualifier("validation-template")
  @Bean
  public Schema createStoredQueryValidatorJsonSchema() {
      InputStream inputStream = QueryTemplateValidator.class.getResourceAsStream(JSON_SCHEMA);
      var jsonSchema = new JSONObject(new JSONTokener(inputStream));
      SchemaLoader loader = SchemaLoader.builder()
          .schemaClient(SchemaClient.classPathAwareClient())
          .schemaJson(jsonSchema)
          .resolutionScope("classpath://query/")
          .draftV7Support()
          .build();
      return loader.load().build();
  }
}
