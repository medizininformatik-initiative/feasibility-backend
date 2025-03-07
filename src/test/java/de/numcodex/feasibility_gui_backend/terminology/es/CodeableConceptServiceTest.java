package de.numcodex.feasibility_gui_backend.terminology.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.dse.api.LocalizedValue;
import de.numcodex.feasibility_gui_backend.terminology.es.model.CodeableConceptDocument;
import de.numcodex.feasibility_gui_backend.terminology.es.model.Display;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.CodeableConceptEsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class CodeableConceptServiceTest {

  @Mock
  ElasticsearchOperations operations;

  @Mock
  CodeableConceptEsRepository repository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private CodeableConceptService codeableConceptService;

  private CodeableConceptService createCodeableConceptService() {
    return new CodeableConceptService(operations, repository);
  }

  @BeforeEach
  void setUp() {
    Mockito.reset(operations, repository);
    codeableConceptService = createCodeableConceptService();
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_succeedsWithoutValueSetFilter() {
    SearchHits<CodeableConceptDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("foo", List.of(), 20, 0));

    assertNotNull(result);
    assertEquals(dummySearchHitsPage.getTotalHits(), result.getTotalHits());
    assertEquals(dummySearchHitsPage.getSearchHits().get(0).getContent().termCode(), result.getResults().get(0).termCode());
    assertEquals(dummySearchHitsPage.getSearchHits().get(0).getContent().display().original(), result.getResults().get(0).display().original());
    assertTrue(result.getResults().get(0).display().translations().containsAll(
            List.of(
                LocalizedValue.builder()
                    .value(dummySearchHitsPage.getSearchHits().get(0).getContent().display().deDe())
                    .language("de-DE")
                    .build(),
                LocalizedValue.builder()
                    .value(dummySearchHitsPage.getSearchHits().get(0).getContent().display().enUs())
                    .language("en-US")
                    .build())
        )
    );
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_succeedsWithNullValueSetFilter() {
    SearchHits<CodeableConceptDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("foo", null, 20, 0));

    assertNotNull(result);
    assertEquals(dummySearchHitsPage.getTotalHits(), result.getTotalHits());
    assertEquals(dummySearchHitsPage.getSearchHits().get(0).getContent().termCode(), result.getResults().get(0).termCode());
    assertEquals(dummySearchHitsPage.getSearchHits().get(0).getContent().display().original(), result.getResults().get(0).display().original());
    assertTrue(result.getResults().get(0).display().translations().containsAll(
        List.of(
            LocalizedValue.builder()
                .value(dummySearchHitsPage.getSearchHits().get(0).getContent().display().deDe())
                .language("de-DE")
                .build(),
            LocalizedValue.builder()
                .value(dummySearchHitsPage.getSearchHits().get(0).getContent().display().enUs())
                .language("en-US")
                .build())
        )
    );
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_succeedsWithValueSetFilter() {
    SearchHits<CodeableConceptDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("foo", List.of("bar"), 20, 0));

    assertNotNull(result);
    assertEquals(dummySearchHitsPage.getTotalHits(), result.getTotalHits());
    assertEquals(dummySearchHitsPage.getSearchHits().get(0).getContent().termCode(), result.getResults().get(0).termCode());
    assertEquals(dummySearchHitsPage.getSearchHits().get(0).getContent().display().original(), result.getResults().get(0).display().original());
    assertTrue(result.getResults().get(0).display().translations().containsAll(
        List.of(
            LocalizedValue.builder()
                .value(dummySearchHitsPage.getSearchHits().get(0).getContent().display().deDe())
                .language("de-DE")
                .build(),
            LocalizedValue.builder()
                .value(dummySearchHitsPage.getSearchHits().get(0).getContent().display().enUs())
                .language("en-US")
                .build())
        )
    );
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_succeedsWithEmptyResult() {
    SearchHits<CodeableConceptDocument> dummySearchHitsPage = createDummySearchHitsPage(0);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("foo", List.of(), 20, 0));

    assertNotNull(result);
    assertThat(result.getTotalHits()).isZero();
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_succeedsWithEmptyKeyword() {
    SearchHits<CodeableConceptDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("", List.of(), 20, 0));

    assertNotNull(result);
    assertThat(result.getTotalHits()).isNotZero();
  }

  @Test
  void testGetSearchResultsEntryByIds_succeeds() {
    CodeableConceptDocument dummyCodeableConceptDocument = createDummyCodeableConceptDocument("1");
    doReturn(List.of(dummyCodeableConceptDocument)).when(repository).findAllById(any());

    var result = assertDoesNotThrow(() -> codeableConceptService.getSearchResultsEntryByIds(List.of("1")));

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(dummyCodeableConceptDocument.termCode(), result.get(0).termCode());
    assertEquals(dummyCodeableConceptDocument.display().original(), result.get(0).display().original());
    assertTrue(result.get(0).display().translations().containsAll(
        List.of(
            LocalizedValue.builder()
                .value(dummyCodeableConceptDocument.display().deDe())
                .language("de-DE")
                .build(),
            LocalizedValue.builder()
                .value(dummyCodeableConceptDocument.display().enUs())
                .language("en-US")
                .build()
        )
    ));
  }

  @Test
  void testGetSearchResultsEntryByIds_emptyOnNotFound() {
    doReturn(List.of()).when(repository).findAllById(anyList());

    var result = assertDoesNotThrow(() -> codeableConceptService.getSearchResultsEntryByIds(List.of("foo")));
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  private SearchHits<CodeableConceptDocument> createDummySearchHitsPage(int totalHits) {
    var searchHitsList = new ArrayList<SearchHit<CodeableConceptDocument>>();

    for (int i = 0; i < totalHits; ++i) {
      searchHitsList.add(
          new SearchHit<>(
              null,
              null,
              null,
              10.0F,
              null,
              null,
              null,
              null,
              null,
              null,
              createDummyCodeableConceptDocument(UUID.randomUUID().toString())
          )
      );
    }
    return new SearchHitsImpl<>(totalHits, TotalHitsRelation.OFF, 10.0F, null, null, null, searchHitsList, null, null, null);
  }

  private CodeableConceptDocument createDummyCodeableConceptDocument(String id) {
    return CodeableConceptDocument.builder()
        .id(id)
        .termCode(createDummyTermcode())
        .display(createDummyDisplay())
        .valueSets(List.of())
        .build();
  }

  private Display createDummyDisplay() {
    return Display.builder()
        .original("code-1")
        .deDe("Code 1")
        .enUs("Code One")
        .build();
  }

  private TermCode createDummyTermcode() {
    return TermCode.builder()
        .code("code-1")
        .display("Code 1")
        .system("http://system1")
        .version("9000")
        .build();
  }
}