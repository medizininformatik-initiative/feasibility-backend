package de.numcodex.feasibility_gui_backend.query.api.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StoredQuery;
import java.io.InputStream;
import javax.validation.ConstraintValidator;
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
public class StoredQueryValidatorSpringConfig {

  private static final String JSON_SCHEMA = "/query/stored-query-schema.json";

  @Value("${app.enableQueryValidation}")
  private boolean enabled;

  @Bean
  public ConstraintValidator<StoredQueryValidation, StoredQuery> createStoredQueryValidator(
          @Qualifier("validation-stored") Schema schema) {
    return enabled
            ? new StoredQueryValidator(schema, new ObjectMapper())
            : new StoredQueryPassValidator();
  }

  @Qualifier("validation-stored")
  @Bean
  public Schema createStoredQueryValidatorJsonSchema() {
      InputStream inputStream = StoredQueryValidator.class.getResourceAsStream(JSON_SCHEMA);
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
