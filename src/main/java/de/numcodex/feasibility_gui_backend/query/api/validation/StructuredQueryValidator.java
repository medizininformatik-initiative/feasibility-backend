package de.numcodex.feasibility_gui_backend.query.api.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Validator for {@link StructuredQuery} that does an actual check based on a JSON schema.
 */
@Slf4j
public class StructuredQueryValidator implements ConstraintValidator<StructuredQueryValidation, StructuredQuery> {

  @NonNull
  private Schema jsonSchema;

  @NonNull
  private ObjectMapper jsonUtil;

  /**
   * Required args constructor.
   *
   * Lombok annotation had to be removed since it could not take the necessary Schema Qualifier
   */
  public StructuredQueryValidator(@Qualifier(value = "validation") Schema jsonSchema, ObjectMapper jsonUtil) {
    this.jsonSchema = jsonSchema;
    this.jsonUtil = jsonUtil;
  }

  /**
   * Validate the submitted {@link StructuredQuery} against the json query schema.
   *
   * @param structuredQuery the {@link StructuredQuery} to validate
   */
  @Override
  public boolean isValid(StructuredQuery structuredQuery,
      ConstraintValidatorContext constraintValidatorContext) {
    try {
      var jsonSubject = new JSONObject(jsonUtil.writeValueAsString(structuredQuery));
      jsonSchema.validate(jsonSubject);
      return true;
    } catch (ValidationException | JsonProcessingException e) {
      log.debug("Structured query is invalid", e);
      return false;
    }
  }
}
