package de.numcodex.feasibility_gui_backend.query.dataquery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.Crtdl;
import de.numcodex.feasibility_gui_backend.query.api.Dataquery;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("query")
@Tag("peristence")
@ExtendWith(MockitoExtension.class)
class DataqueryHandlerTest {

    public static final String CREATOR = "creator-557261";
    public static final String LABEL = "some-label";
    public static final String COMMENT = "some-comment";
    public static final String TIME_STRING = "1969-07-20 20:17:40.0";
    private static final int MAX_QUERIES_PER_USER = 5;

    @Spy
    private ObjectMapper jsonUtil = new ObjectMapper();

    @Mock
    private DataqueryRepository dataqueryRepository;

    private DataqueryHandler createDataqueryHandler() {
        return new DataqueryHandler(jsonUtil, dataqueryRepository, MAX_QUERIES_PER_USER);
    }

    @Test
    @DisplayName("storeDataquery() -> trying to store a valid object with a user does not throw")
    void storeDataquery_succeeds() throws JsonProcessingException {
        var dataqueryHandler = createDataqueryHandler();
        doReturn(createDataqueryEntity()).when(dataqueryRepository).save(any());

        assertDoesNotThrow(() -> dataqueryHandler.storeDataquery(createDataquery(), CREATOR));
    }

    @Test
    @DisplayName("storeDataquery() -> trying to store a null object with a null user throws an exception")
    void storeDataquery_throwsOnEmptyQueryAndEmptyUser() {
        var dataqueryHandler = createDataqueryHandler();
        assertThrows(NullPointerException.class, () -> dataqueryHandler.storeDataquery(null, null));
    }

    @Test
    @DisplayName("storeDataquery() -> trying to store a null object with a user throws an exception")
    void storeDataquery_throwsOnEmptyQueryAndNonEmptyUser() {
        var dataqueryHandler = createDataqueryHandler();
        assertThrows(NullPointerException.class, () -> dataqueryHandler.storeDataquery(null, CREATOR));
    }

    @Test
    @DisplayName("storeDataquery() -> trying to store an object with a null user throws an exception")
    void storeDataquery_throwsOnNonEmptyQueryAndEmptyUser() {
        var dataqueryHandler = createDataqueryHandler();
        assertThrows(NullPointerException.class, () -> dataqueryHandler.storeDataquery(createDataquery(), null));
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    @DisplayName("storeDataquery() -> trying to store a dataquery when no slots are free throws")
    void storeDataquery_throwsOnNoFreeSlots(boolean withResult) throws JsonProcessingException {
        var dataqueryHandler = createDataqueryHandler();
        lenient().doReturn(MAX_QUERIES_PER_USER + 1L).when(dataqueryRepository).countByCreatedByWhereResultIsNotNull(any(String.class));
        lenient().doReturn(createDataqueryEntity()).when(dataqueryRepository).save(any(de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.class));

        if (withResult) {
            assertThrows(DataqueryStorageFullException.class, () -> dataqueryHandler.storeDataquery(createDataquery(withResult), CREATOR));
        } else {
            assertDoesNotThrow(() -> dataqueryHandler.storeDataquery(createDataquery(withResult), CREATOR));
        }
    }

    @ParameterizedTest
    @CsvSource({"true,-1", "true,0", "true,1", "false,-1", "false,0", "false,1"})
    @DisplayName("storeDataquery() -> checking around the query limit")
    void storeDataquery_testFreeSlotOnEdgeCases(boolean withResult, long offset) throws JsonProcessingException {
        var dataqueryHandler = createDataqueryHandler();
        lenient().doReturn(MAX_QUERIES_PER_USER + offset).when(dataqueryRepository).countByCreatedByWhereResultIsNotNull(any(String.class));
        lenient().doReturn(createDataqueryEntity()).when(dataqueryRepository).save(any(de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.class));

        if (withResult) {
            if (offset < 0) {
                assertDoesNotThrow(() -> dataqueryHandler.storeDataquery(createDataquery(withResult), CREATOR));
            } else {
                assertThrows(DataqueryStorageFullException.class, () -> dataqueryHandler.storeDataquery(createDataquery(withResult), CREATOR));
            }
        } else {
            assertDoesNotThrow(() -> dataqueryHandler.storeDataquery(createDataquery(withResult), CREATOR));
        }
    }

    @Test
    @DisplayName("storeDataquery() -> error in json serialization throws an exception")
    void storeDataquery_throwsOnJsonSerializationError() throws JsonProcessingException {
        var dataquery = createDataquery();
        doThrow(JsonProcessingException.class).when(jsonUtil).writeValueAsString(any(Crtdl.class));

        var dataqueryHandler = createDataqueryHandler();
        assertThrows(DataqueryException.class, () -> dataqueryHandler.storeDataquery(dataquery, CREATOR));
    }

    @Test
    @DisplayName("getDataqueryById() -> retrieving a single dataquery by its id succeeds")
    void getDataqueryById_succeeds() throws JsonProcessingException {
        var dataqueryHandler = createDataqueryHandler();
        var dataqueryEntity = createDataqueryEntity();
        doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));

        Dataquery dataquery = assertDoesNotThrow(() -> dataqueryHandler.getDataqueryById(1L, CREATOR));
        assertNotNull(dataquery);
        assertInstanceOf(Dataquery.class, dataquery);
    }

    @Test
    @DisplayName("getDataqueryById() -> Throw exception if the user is not the author")
    void getDataqueryById_throwsOnWrongUser() throws JsonProcessingException {
        var dataqueryHandler = createDataqueryHandler();
        var dataqueryEntity = createDataqueryEntity();
        doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));

        assertThrows(DataqueryException.class, () -> dataqueryHandler.getDataqueryById(1L, "NOT THE " + CREATOR));
    }

    @Test
    @DisplayName("getDataqueryById() -> Throw exception if the query does not exist")
    void getDataqueryById_throwsOnNotFound() {
        var dataqueryHandler = createDataqueryHandler();
        doReturn(Optional.empty()).when(dataqueryRepository).findById(any(Long.class));

        assertThrows(DataqueryException.class, () -> dataqueryHandler.getDataqueryById(1L, CREATOR));
    }

    @Test
    @DisplayName("updateDataquery() -> trying to update a dataquery succeeds")
    void updateDataquery_succeeds() throws JsonProcessingException {
        var dataqueryHandler = createDataqueryHandler();
        var dataquery = createDataquery();
        var dataqueryEntity = createDataqueryEntity();

        doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));

        assertDoesNotThrow(() -> dataqueryHandler.updateDataquery(1L, dataquery, CREATOR));
    }

    @Test
    @DisplayName("updateDataquery() -> throws exception when actor is not the original creator")
    void updateDataquery_throwsOnWrongUser() throws JsonProcessingException {
        var dataqueryHandler = createDataqueryHandler();
        var dataquery = createDataquery();
        var dataqueryEntity = createDataqueryEntity();
        doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));

        assertThrows(DataqueryException.class, () -> dataqueryHandler.updateDataquery(1L, dataquery, "NOT THE " + CREATOR));
    }

    @Test
    @DisplayName("updateDataquery() -> throws exception when query is not found")
    void updateDataquery_throwsOnNotFound() {
        var dataqueryHandler = createDataqueryHandler();
        doReturn(Optional.empty()).when(dataqueryRepository).findById(any(Long.class));

        assertThrows(DataqueryException.class, () -> dataqueryHandler.updateDataquery(1L, createDataquery(), CREATOR));
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    @DisplayName("updateDataquery() -> check if storage full exceptions are thrown correctly")
    void updateDataquery_throwsOnNoFreeSlots(boolean withResultNew, boolean withResultOld) throws JsonProcessingException {
        var dataqueryHandler = createDataqueryHandler();
        var dataquery = createDataquery(withResultNew);
        var dataqueryEntity = createDataqueryEntity(withResultOld);

        lenient().doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));
        lenient().doReturn((long) MAX_QUERIES_PER_USER).when(dataqueryRepository).countByCreatedByWhereResultIsNotNull(any(String.class));
        lenient().doReturn(createDataqueryEntity()).when(dataqueryRepository).save(any(de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.class));

        // When the new dataquery has no result, this should never throw. It should only throw if the old had no result and the new has a result
        if (withResultNew && !withResultOld) {
            assertThrows(DataqueryStorageFullException.class, () -> dataqueryHandler.updateDataquery(1L, dataquery, CREATOR));
        } else {
            assertDoesNotThrow(() -> dataqueryHandler.updateDataquery(1L, dataquery, CREATOR));
        }
    }

    @ParameterizedTest
    @CsvSource({
        "-1,true,true", "-1,true,false", "-1,false,true", "-1,false,false",
        "0,true,true", "0,true,false", "0,false,true", "0,false,false",
        "1,true,true", "1,true,false", "1,false,true", "1,false,false"
    })
    @DisplayName("updateDataquery() -> checking around the query limit")
    void updateDataquery_testFreeSlotOnEdgeCases(long offset, boolean withResultNew, boolean withResultOld) throws JsonProcessingException {
        var dataqueryHandler = createDataqueryHandler();
        var dataquery = createDataquery(withResultNew);
        var dataqueryEntity = createDataqueryEntity(withResultOld);

        lenient().doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));
        lenient().doReturn(MAX_QUERIES_PER_USER + offset).when(dataqueryRepository).countByCreatedByWhereResultIsNotNull(any(String.class));
        lenient().doReturn(createDataqueryEntity()).when(dataqueryRepository).save(any(de.numcodex.feasibility_gui_backend.query.persistence.Dataquery.class));

        // It should only throw when the new query has a result, the old didn't have one and the storage is full
        // If the storage is full but the old query already had a result, it should not fail.
        if (withResultNew && !withResultOld && offset >= 0) {
            assertThrows(DataqueryStorageFullException.class, () -> dataqueryHandler.updateDataquery(1L, dataquery, CREATOR));
        } else {
            assertDoesNotThrow(() -> dataqueryHandler.updateDataquery(1L, dataquery, CREATOR));
        }
    }

    @Test
    @DisplayName("getDataqueriesByAuthor() -> succeeds")
    void getDataqueriesByAuthor_succeedsWithEntry() throws JsonProcessingException {
        var dataqueryHandler = createDataqueryHandler();
        var dataqueryEntity = createDataqueryEntity();

        doReturn(List.of(dataqueryEntity)).when(dataqueryRepository).findAllByCreatedBy(any(String.class));

        List<Dataquery> dataqueries = assertDoesNotThrow(() -> dataqueryHandler.getDataqueriesByAuthor(CREATOR));

        assertNotNull(dataqueries);
        assertEquals(1, dataqueries.size());
        assertEquals(dataqueryEntity.getLabel(), dataqueries.get(0).label());
        assertEquals(dataqueryEntity.getComment(), dataqueries.get(0).comment());
    }

    @Test
    @DisplayName("getDataqueriesByAuthor() -> succeeds with empty list")
    void getDataqueriesByAuthor_succeedsWithEmptyList() throws JsonProcessingException {
        var dataqueryHandler = createDataqueryHandler();

        doReturn(List.of()).when(dataqueryRepository).findAllByCreatedBy(any(String.class));

        List<Dataquery> dataqueries = assertDoesNotThrow(() -> dataqueryHandler.getDataqueriesByAuthor(CREATOR));

        assertNotNull(dataqueries);
        assertEquals(0, dataqueries.size());
    }

    @Test
    @DisplayName("getDataqueriesByAuthor() -> throws on json error")
    void getDataqueriesByAuthor_throwsOnJsonException() throws JsonProcessingException {
        var dataqueryHandler = createDataqueryHandler();
        var dataqueryEntity = createDataqueryEntity();

        doReturn(List.of(dataqueryEntity)).when(dataqueryRepository).findAllByCreatedBy(any(String.class));
        doThrow(JsonProcessingException.class).when(jsonUtil).readValue(any(String.class), any(Class.class));

        assertThrows(DataqueryException.class, () -> dataqueryHandler.getDataqueriesByAuthor(CREATOR));
    }

    @Test
    @DisplayName("deleteDataquery() -> succeeds")
    void deleteDataquery_succeeds() throws JsonProcessingException {
        var dataqueryHandler = createDataqueryHandler();
        var dataqueryEntity = createDataqueryEntity();

        doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));

        assertDoesNotThrow(() -> dataqueryHandler.deleteDataquery(1L, CREATOR));
    }

    @Test
    @DisplayName("deleteDataquery() -> throws on wrong user")
    void deleteDataquery_throwsOnWrongUser() throws JsonProcessingException {
        var dataqueryHandler = createDataqueryHandler();
        var dataqueryEntity = createDataqueryEntity();

        doReturn(Optional.of(dataqueryEntity)).when(dataqueryRepository).findById(any(Long.class));

        assertThrows(DataqueryException.class, () -> dataqueryHandler.deleteDataquery(1L, "NOT THE " +CREATOR));
    }

    @Test
    @DisplayName("getDataquerySlotsJson() -> succeeds")
    void getDataquerySlotsJson_succeeds() throws JsonProcessingException {
        var dataqueryHandler = createDataqueryHandler();
        var usedSlots = new Random().nextLong();

        doReturn(usedSlots).when(dataqueryRepository).countByCreatedByWhereResultIsNotNull(any(String.class));

        var savedQuerySlots = assertDoesNotThrow(() -> dataqueryHandler.getDataquerySlotsJson(CREATOR));

        assertEquals(usedSlots, savedQuerySlots.used());
    }

    @Test
    @DisplayName("convertApiToPersistence() -> converting a dataquery from the rest api to the format that will be stored in the database succeeds")
    void convertApiToPersistence() throws JsonProcessingException {
        var dataQuery = createDataquery();
        var dataqueryHandler = createDataqueryHandler();

        var convertedDataquery = dataqueryHandler.convertApiToPersistence(dataQuery);
        var convertedCrtdl = jsonUtil.readValue(convertedDataquery.getCrtdl(), Crtdl.class);

        assertEquals(convertedDataquery.getId(), dataQuery.id());
        assertEquals(convertedDataquery.getLabel(), dataQuery.label());
        assertEquals(convertedDataquery.getComment(), dataQuery.comment());
        assertEquals(convertedDataquery.getLastModified().toString(), dataQuery.lastModified());
        assertEquals(convertedDataquery.getResultSize(), dataQuery.resultSize());
        assertEquals(convertedCrtdl.display(), dataQuery.content().display());
        assertEquals(convertedCrtdl.version(), dataQuery.content().version());
        assertEquals(convertedCrtdl.cohortDefinition().display(), dataQuery.content().cohortDefinition().display());
        assertEquals(convertedCrtdl.cohortDefinition().version(), dataQuery.content().cohortDefinition().version());
        assertEquals(convertedCrtdl.cohortDefinition().inclusionCriteria(), dataQuery.content().cohortDefinition().inclusionCriteria());
        assertEquals(convertedCrtdl.cohortDefinition().exclusionCriteria(), dataQuery.content().cohortDefinition().exclusionCriteria());
    }

    @Test
    @DisplayName("convertPersistenceToApi() -> converting a dataquery from the database to the format that will be sent out via api succeeds")
    void convertPersistenceToApi() throws JsonProcessingException {
        var dataqueryEntity = createDataqueryEntity();
        var dataqueryHandler = createDataqueryHandler();
        var convertedDataquery = dataqueryHandler.convertPersistenceToApi(dataqueryEntity);
        var originalCrtdl = jsonUtil.readValue(dataqueryEntity.getCrtdl(), Crtdl.class);

        assertEquals(convertedDataquery.id(), dataqueryEntity.getId());
        assertEquals(convertedDataquery.label(), dataqueryEntity.getLabel());
        assertEquals(convertedDataquery.comment(), dataqueryEntity.getComment());
        assertEquals(convertedDataquery.createdBy(), dataqueryEntity.getCreatedBy());
        assertEquals(convertedDataquery.lastModified(), dataqueryEntity.getLastModified().toString());
        assertEquals(convertedDataquery.content().display(), originalCrtdl.display());
        assertEquals(convertedDataquery.content().version(), originalCrtdl.version());
        assertEquals(convertedDataquery.content().cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).code(),
            originalCrtdl.cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).code());
        assertEquals(convertedDataquery.content().cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).display(),
            originalCrtdl.cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).display());
        assertEquals(convertedDataquery.content().cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).version(),
            originalCrtdl.cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).version());
        assertEquals(convertedDataquery.content().cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).system(),
            originalCrtdl.cohortDefinition().inclusionCriteria().get(0).get(0).termCodes().get(0).system());
        assertEquals(convertedDataquery.content().cohortDefinition().exclusionCriteria(), originalCrtdl.cohortDefinition().exclusionCriteria());
    }

    private Dataquery createDataquery(boolean withResult) {
        return Dataquery.builder()
            .id(1L)
            .label(LABEL)
            .comment(COMMENT)
            .content(createCrtdl())
            .resultSize(withResult ? 123L : null)
            .createdBy(CREATOR)
            .lastModified(TIME_STRING)
            .build();
    }

    private Dataquery createDataquery() {
        return createDataquery(false);
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
        out.setId(1L);
        out.setLabel(LABEL);
        out.setComment(COMMENT);
        out.setLastModified(Timestamp.valueOf(TIME_STRING));
        out.setCreatedBy(CREATOR);
        out.setResultSize(withResult ? 123L : null);
        out.setCrtdl(jsonUtil.writeValueAsString(createCrtdl()));
        return out;
    }

    private de.numcodex.feasibility_gui_backend.query.persistence.Dataquery createDataqueryEntity() throws JsonProcessingException {
        return  createDataqueryEntity(false);
    }
}
