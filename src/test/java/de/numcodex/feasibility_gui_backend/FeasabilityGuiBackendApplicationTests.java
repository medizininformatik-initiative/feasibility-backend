package de.numcodex.feasibility_gui_backend;

import QueryBuilderMoc.QueryBuilderCQL;
import QueryBuilderMoc.QueryBuilderFHIR;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.model.query.QueryDefinition;
import de.numcodex.feasibility_gui_backend.model.ui.TerminologyEntry;
import de.numcodex.feasibility_gui_backend.service.QueryBuilderService;
import java.net.URI;
import java.net.URL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class FeasibilityGuiBackendApplicationTests {

  @Test
  public void fromJson() throws Exception {
    var objectMapper = new ObjectMapper();

    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    QueryDefinition queryDefinition = objectMapper.readValue(
        new URL("file:src/test/resources/2021_02_01_QueryExampleWithoutTimeRestriction.json"),
        QueryDefinition.class);
    Assertions.assertEquals(new URI("http://to_be_decided.com/draft-1/schema#"),
        queryDefinition.getVersion());
  }

  @Test
  public void queryBuilder() {
    var queryService = new QueryBuilderService();
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

    System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(demographics));
  }

}