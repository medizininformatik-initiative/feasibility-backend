package de.numcodex.feasibility_gui_backend.service.query_builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;

//Not needed for now?
public interface QueryBuilder {

  String getQueryContent(StructuredQuery query) throws QueryBuilderException;
}
