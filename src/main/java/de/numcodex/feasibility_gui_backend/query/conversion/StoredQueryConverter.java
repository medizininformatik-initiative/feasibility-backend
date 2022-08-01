package de.numcodex.feasibility_gui_backend.query.conversion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import java.sql.Timestamp;

public class StoredQueryConverter {

  private StoredQueryConverter() {

  }

  public static de.numcodex.feasibility_gui_backend.query.persistence.StoredQuery convertApiToPersistence(
      de.numcodex.feasibility_gui_backend.query.api.StoredQuery in) throws JsonProcessingException {
    de.numcodex.feasibility_gui_backend.query.persistence.StoredQuery out = new de.numcodex.feasibility_gui_backend.query.persistence.StoredQuery();

    ObjectMapper jsonUtil = new ObjectMapper();
    out.setQueryContent(jsonUtil.writeValueAsString(in.getStructuredQuery()));
    out.setComment(in.getComment());
    out.setLabel(in.getLabel());
    out.setCreatedBy(in.getCreatedBy());
    if (in.getLastModified() != null) {
      out.setLastModified(Timestamp.valueOf(in.getLastModified()));
    }
    return out;
  }

  public static de.numcodex.feasibility_gui_backend.query.api.StoredQuery convertPersistenceToApi(
      de.numcodex.feasibility_gui_backend.query.persistence.StoredQuery in)
      throws JsonProcessingException {
    de.numcodex.feasibility_gui_backend.query.api.StoredQuery out = new de.numcodex.feasibility_gui_backend.query.api.StoredQuery();

    ObjectMapper jsonUtil = new ObjectMapper();
    out.setComment(in.getComment());
    out.setLabel(in.getLabel());
    out.setStructuredQuery(jsonUtil.readValue(in.getQueryContent(), StructuredQuery.class));
    out.setLastModified(in.getLastModified().toString());
    out.setCreatedBy(in.getCreatedBy());
    out.setId(in.getId());
    return out;
  }
}
