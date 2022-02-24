package de.numcodex.feasibility_gui_backend.query.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

@Slf4j
@RequiredArgsConstructor
public class QueryValidator {

  @NonNull
  private ObjectMapper jsonUtil;

  private static final String JSON_SCHEMA = "/query/query-schema.json";

  /**
   * Validate the submitted {@link StructuredQuery} against the json query schema.
   *
   * @param structuredQuery the {@link StructuredQuery} to validate
   * @throws ValidationException in case the submitted {@link StructuredQuery} does not comply with
   * the schema
   * @throws FileNotFoundException if the JSON schema file can not be found or read
   * @throws JSONException in case the JSON schema file is not a valid JSON file
   */
  public void validate(StructuredQuery structuredQuery)
      throws ValidationException, FileNotFoundException, JSONException {
    try (InputStream inputStream = QueryValidator.class.getResourceAsStream(JSON_SCHEMA)) {
      var jsonSchema = new JSONObject(new JSONTokener(inputStream));
      var jsonSubject = new JSONObject(jsonUtil.writeValueAsString(structuredQuery));
      SchemaLoader loader = SchemaLoader.builder()
          .schemaJson(jsonSchema)
          .draftV7Support()
          .build();
      var schema = loader.load().build();
      schema.validate(jsonSubject);
    } catch (IOException | NullPointerException e) {
      throw new FileNotFoundException();
    }
  }
}
