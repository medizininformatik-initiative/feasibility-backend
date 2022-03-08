package de.numcodex.feasibility_gui_backend.query.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;

@RequiredArgsConstructor
@Slf4j
public class QueryValidator implements ConstraintValidator<JsonSchemaValidation, StructuredQuery> {

  @NonNull
  private Schema jsonSchema;

  @NonNull
  private ObjectMapper jsonUtil;

  @Setter
  private boolean enabled = true;

  /**
   * Validate the submitted {@link StructuredQuery} against the json query schema.
   *
   * @param structuredQuery the {@link StructuredQuery} to validate
   */
  @Override
  public boolean isValid(StructuredQuery structuredQuery,
      ConstraintValidatorContext constraintValidatorContext) {
    if (!enabled) {
      log.debug("Query validation disabled.");
      return true;
    }
    try {
      var jsonSubject = new JSONObject(jsonUtil.writeValueAsString(structuredQuery));
      jsonSchema.validate(jsonSubject);
      return true;
    } catch (ValidationException | JsonProcessingException e) {
      log.debug("Query validation failed: {}", e.getMessage());
      return false;
    }
  }
}
