package de.numcodex.feasibility_gui_backend;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.model.ui.TerminologyEntry;
import de.numcodex.feasibility_gui_backend.service.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.service.query_builder.QueryBuilderCQL;
import de.numcodex.feasibility_gui_backend.service.query_builder.QueryBuilderFHIR;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;


class FeasibilityGuiBackendApplicationTests {

  //@Test
  public void fromJson() throws Exception {
    var objectMapper = new ObjectMapper();

    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    StructuredQuery structuredQuery = objectMapper.readValue(
        new URL("file:src/test/resources/2021_02_01_QueryExampleWithoutTimeRestriction.json"),
        StructuredQuery.class);
    Assertions.assertEquals(new URI("http://to_be_decided.com/draft-1/schema#"),
        structuredQuery.getVersion());
  }

  @Test
  public void queryBuilder() {
    var queryService = new QueryHandlerService();
    var cqlBuilder = new QueryBuilderCQL();
    var fhirBuilder = new QueryBuilderFHIR();
    Assertions.assertEquals("FHIRQuery", queryService.getQueryContent(fhirBuilder));
    Assertions.assertEquals("CQLQuery", queryService.getQueryContent(cqlBuilder));
  }


  @Test
  public void terminologyEntryFromJson() throws Exception {
    var objectMapper = new ObjectMapper();

    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    TerminologyEntry demographics = objectMapper.readValue(
        new URL("file:src/test/resources/Demographie.json"),
        TerminologyEntry.class);

    System.out
        .println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(demographics));
  }

}
