package de.numcodex.feasibility_gui_backend.query.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class QueryValidatorSpringConfig {

  private static final String JSON_SCHEMA = "/query/query-schema.json";

  @Value("${app.enableQueryValidation}")
  private boolean enabled;

  @Bean
  public QueryValidator createQueryValidator( @Qualifier("validation") Schema schema) {
    QueryValidator queryValidator = new QueryValidator(schema, new ObjectMapper());
    queryValidator.setEnabled(enabled);
    return queryValidator;
  }

  @Qualifier("validation")
  @Bean
  public Schema createQueryValidatorJsonSchema() throws FileNotFoundException {
    try (InputStream inputStream = QueryValidator.class.getResourceAsStream(JSON_SCHEMA)) {
      var jsonSchema = new JSONObject(new JSONTokener(inputStream));
      SchemaLoader loader = SchemaLoader.builder()
          .schemaJson(jsonSchema)
          .draftV7Support()
          .build();
      return loader.load().build();
    } catch (IOException | NullPointerException e) {
      log.error("JSON schema file for sq could not be read.");
      throw new FileNotFoundException();
    }
  }
}
