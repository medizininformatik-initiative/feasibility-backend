package de.numcodex.feasibility_gui_backend.query.api.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.ConstraintValidator;
import java.io.InputStream;

@Configuration
@Slf4j
public class StructuredQueryValidatorSpringConfig {

  private static final String JSON_SCHEMA = "/de/numcodex/feasibility_gui_backend/query/api/validation/query-schema.json";

  @Value("${app.enableQueryValidation}")
  private boolean enabled;

  @Bean
  public ConstraintValidator<StructuredQueryValidation, StructuredQuery> createQueryValidator(
          @Qualifier("validation") Schema schema) {
    return enabled
            ? new StructuredQueryValidator(schema, new ObjectMapper())
            : new StructuredQueryPassValidator();
  }

  @Qualifier("validation")
  @Bean
  public Schema createQueryValidatorJsonSchema() {
      InputStream inputStream = StructuredQueryValidator.class.getResourceAsStream(JSON_SCHEMA);
      var jsonSchema = new JSONObject(new JSONTokener(inputStream));
      SchemaLoader loader = SchemaLoader.builder()
          .schemaJson(jsonSchema)
          .draftV7Support()
          .build();
      return loader.load().build();
  }
}
