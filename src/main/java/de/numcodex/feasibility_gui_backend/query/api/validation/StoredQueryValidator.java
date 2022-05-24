package de.numcodex.feasibility_gui_backend.query.api.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StoredQuery;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Validator for {@link StoredQuery} that does an actual check based on a JSON schema.
 */
@Slf4j
public class StoredQueryValidator implements ConstraintValidator<StoredQueryValidation, StoredQuery> {

  @NonNull
  private Schema jsonSchema;

  @NonNull
  private ObjectMapper jsonUtil;

  /**
   * Required args constructor.
   *
   * Lombok annotation had to be removed since it could not take the necessary Schema Qualifier
   */
  public StoredQueryValidator(@Qualifier(value = "validation-stored") Schema jsonSchema, ObjectMapper jsonUtil) {
    this.jsonSchema = jsonSchema;
    this.jsonUtil = jsonUtil;
  }

  /**
   * Validate the submitted {@link StoredQuery} against the json query schema.
   *
   * @param storedQuery the {@link StoredQuery} to validate
   */
  @Override
  public boolean isValid(StoredQuery storedQuery,
      ConstraintValidatorContext constraintValidatorContext) {
    try {
      var jsonSubject = new JSONObject(jsonUtil.writeValueAsString(storedQuery));
      jsonSchema.validate(jsonSubject);
      return true;
    } catch (ValidationException | JsonProcessingException e) {
      log.debug("Stored query is invalid", e);
      return false;
    }
  }
}
