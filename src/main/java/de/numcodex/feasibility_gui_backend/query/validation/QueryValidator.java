package de.numcodex.feasibility_gui_backend.query.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
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

  public void validate(StructuredQuery structuredQuery)
      throws ValidationException, FileNotFoundException, JSONException {
    try (InputStream inputStream = QueryValidator.class.getResourceAsStream(JSON_SCHEMA)) {
      JSONObject jsonSchema = new JSONObject(new JSONTokener(inputStream));
      JSONObject jsonSubject = new JSONObject(jsonUtil.writeValueAsString(structuredQuery));
      Schema schema = SchemaLoader.load(jsonSchema);
      schema.validate(jsonSubject);
    } catch (IOException | NullPointerException e) {
      throw new FileNotFoundException();
    }
  }
}
