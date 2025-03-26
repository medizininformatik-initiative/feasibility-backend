package de.numcodex.feasibility_gui_backend.query.dataquery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.api.Crtdl;
import de.numcodex.feasibility_gui_backend.query.api.Dataquery;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.status.SavedQuerySlots;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerSpringConfig;
import de.numcodex.feasibility_gui_backend.query.collect.QueryCollectSpringConfig;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchSpringConfig;
import de.numcodex.feasibility_gui_backend.query.persistence.DataqueryRepository;
import de.numcodex.feasibility_gui_backend.query.result.ResultServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslatorSpringConfig;
import de.numcodex.feasibility_gui_backend.terminology.validation.StructuredQueryValidation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Tag("query")
@Tag("handler")
@Import({
    BrokerSpringConfig.class,
    QueryTranslatorSpringConfig.class,
    QueryDispatchSpringConfig.class,
    QueryCollectSpringConfig.class,
    QueryHandlerService.class,
    ResultServiceSpringConfig.class,
    DataquerySpringConfig.class
})
@DataJpaTest(
    properties = {
        "app.cqlTranslationEnabled=false",
        "app.fhirTranslationEnabled=false",
        "app.broker.mock.enabled=true",
        "app.broker.direct.enabled=false",
        "app.broker.aktin.enabled=false",
        "app.broker.dsf.enabled=false"
    }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class DataqueryHandlerIT {
  public static final String SITE_NAME_1 = "site-name-114606";
  public static final String SITE_NAME_2 = "site-name-114610";
  public static final String CREATOR = "creator-114634";
  public static final long UNKNOWN_QUERY_ID = 9999999L;
  public static final String LABEL = "some-label";
  public static final String COMMENT = "some-comment";
  public static final String TIME_STRING = "1969-07-20 20:17:40.0";

  @Autowired
  private DataqueryHandler dataqueryHandler;

  @Autowired
  private DataqueryRepository dataqueryRepository;

  @MockitoBean
  private StructuredQueryValidation structuredQueryValidation;

  @Autowired
  @Qualifier("translation")
  private ObjectMapper jsonUtil;

  @Test
  public void testStoreDataquery() throws DataqueryStorageFullException, DataqueryException {
    var testDataquery = createDataquery(false);

    dataqueryHandler.storeDataquery(testDataquery, "test");

    assertThat(dataqueryRepository.count()).isOne();
  }

  @Test
  public void testGetDataquery() throws JsonProcessingException {
    var dataqueryEntity = createDataqueryEntity(false);
    var dataqueryId = dataqueryRepository.save(dataqueryEntity).getId();

    var loadedDataquery = assertDoesNotThrow(() -> dataqueryHandler.getDataqueryById(dataqueryId, dataqueryEntity.getCreatedBy()));

    assertThat(loadedDataquery).isNotNull();
    assertThat(jsonUtil.writeValueAsString(loadedDataquery.content())).isEqualTo(dataqueryEntity.getCrtdl());
  }

  @Test
  public void testUpdateDataquery() throws JsonProcessingException, DataqueryStorageFullException, DataqueryException {
    var dataquery = createDataquery(false);
    var dataqueryWithResult = createDataquery(true);

    var dataqueryId = dataqueryHandler.storeDataquery(dataquery, CREATOR);
    var loadedDataqueryOld = assertDoesNotThrow(() -> dataqueryHandler.getDataqueryById(dataqueryId, CREATOR));
    dataqueryHandler.updateDataquery(dataqueryId, dataqueryWithResult, CREATOR);
    var loadedDataqueryUpdated = assertDoesNotThrow(() -> dataqueryHandler.getDataqueryById(dataqueryId, CREATOR));

    assertThat(loadedDataqueryUpdated).isNotNull();
    assertThat(loadedDataqueryUpdated.resultSize()).isEqualTo(dataqueryWithResult.resultSize());
    assertThat(loadedDataqueryUpdated.resultSize()).isNotEqualTo(loadedDataqueryOld.resultSize());
  }

  @Test
  public void testGetDataqueriesByAuthor() throws JsonProcessingException {
    var listSize = 10;

    for (int i = 0; i < listSize; ++i) {
      dataqueryRepository.save(createDataqueryEntity(false));
    }

    var dataqueryList = assertDoesNotThrow(() -> dataqueryHandler.getDataqueriesByAuthor(CREATOR));
    var dataqueryListOtherAuthor = assertDoesNotThrow(() -> dataqueryHandler.getDataqueriesByAuthor("other-" + CREATOR));

    assertThat(dataqueryList.size()).isEqualTo(listSize);
    assertThat(dataqueryListOtherAuthor).isEmpty();
  }

  @Test
  public void testDeleteDataquery() throws JsonProcessingException {
    var listSize = 10;

    for (int i = 0; i < listSize; ++i) {
      dataqueryRepository.save(createDataqueryEntity(false));
    }

    var dataqueryListBeforeDelete = assertDoesNotThrow(() -> dataqueryHandler.getDataqueriesByAuthor(CREATOR));

    assertDoesNotThrow(() -> dataqueryHandler.deleteDataquery(dataqueryListBeforeDelete.get(0).id(), CREATOR));

    var dataqueryListAfterDelete = assertDoesNotThrow(() -> dataqueryHandler.getDataqueriesByAuthor(CREATOR));

    assertThat(dataqueryListBeforeDelete.size()).isEqualTo(listSize);
    assertThat(dataqueryListAfterDelete.size()).isEqualTo(listSize - 1);
  }

  @Test
  public void testGetDataquerySlotsJson() throws JsonProcessingException, DataqueryException {
    var dataqueriesWithResult = 3;
    var dataqueriesWithoutResult = 5;

    for (int i = 0; i < dataqueriesWithResult; ++i) {
      dataqueryRepository.save(createDataqueryEntity(true));
    }

    for (int i = 0; i < dataqueriesWithoutResult; ++i) {
      dataqueryRepository.save(createDataqueryEntity(false));
    }

    var dataqueryList = dataqueryHandler.getDataqueriesByAuthor(CREATOR);
    var dataquerySlots = assertDoesNotThrow(() -> dataqueryHandler.getDataquerySlotsJson(CREATOR));

    assertThat(dataqueryList.size()).isEqualTo(dataqueriesWithoutResult + dataqueriesWithResult);
    assertThat(dataquerySlots).isInstanceOf(SavedQuerySlots.class);
    assertThat(dataquerySlots.used()).isEqualTo(dataqueriesWithResult);
  }

  private Dataquery createDataquery(boolean withResult) {
    return Dataquery.builder()
        .label("test")
        .comment("test")
        .content(createCrtdl())
        .resultSize(withResult ? 123L : null)
        .build();
  }

  private Crtdl createCrtdl() {
    return Crtdl.builder()
        .cohortDefinition(createValidStructuredQuery())
        .display("foo")
        .build();
  }

  private StructuredQuery createValidStructuredQuery() {
    var termCode = TermCode.builder()
        .code("LL2191-6")
        .system("http://loinc.org")
        .display("Geschlecht")
        .build();
    var inclusionCriterion = Criterion.builder()
        .termCodes(List.of(termCode))
        .attributeFilters(List.of())
        .build();
    return StructuredQuery.builder()
        .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
        .inclusionCriteria(List.of(List.of(inclusionCriterion)))
        .exclusionCriteria(null)
        .display("foo")
        .build();
  }

  private de.numcodex.feasibility_gui_backend.query.persistence.Dataquery createDataqueryEntity(boolean withResult) throws JsonProcessingException {
    de.numcodex.feasibility_gui_backend.query.persistence.Dataquery out = new de.numcodex.feasibility_gui_backend.query.persistence.Dataquery();
    out.setLabel(LABEL);
    out.setComment(COMMENT);
    out.setLastModified(Timestamp.valueOf(TIME_STRING));
    out.setCreatedBy(CREATOR);
    out.setResultSize(withResult ? 123L : null);
    out.setCrtdl(jsonUtil.writeValueAsString(createCrtdl()));
    return out;
  }
}
