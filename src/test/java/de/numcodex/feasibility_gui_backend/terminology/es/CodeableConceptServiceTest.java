package de.numcodex.feasibility_gui_backend.terminology.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.terminology.es.model.CodeableConceptDocument;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.CodeableConceptEsRepository;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemNotFoundException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    assertEquals(dummySearchHitsPage.getSearchHits().get(0).getContent().termCode(), result.getResults().get(0));
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_succeedsWithNullValueSetFilter() {
    SearchHits<CodeableConceptDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("foo", null, 20, 0));

    assertNotNull(result);
    assertEquals(dummySearchHitsPage.getTotalHits(), result.getTotalHits());
    assertEquals(dummySearchHitsPage.getSearchHits().get(0).getContent().termCode(), result.getResults().get(0));
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_succeedsWithValueSetFilter() {
    SearchHits<CodeableConceptDocument> dummySearchHitsPage = createDummySearchHitsPage(5);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var result = assertDoesNotThrow(() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("foo", List.of("bar"), 20, 0));

    assertNotNull(result);
    assertEquals(dummySearchHitsPage.getTotalHits(), result.getTotalHits());
    assertEquals(dummySearchHitsPage.getSearchHits().get(0).getContent().termCode(), result.getResults().get(0));
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
  void testGetSearchResultEntryByCode_succeeds() {
    CodeableConceptDocument dummyCodeableConceptDocument = createDummyCodeableConceptDocument("1");
    doReturn(Optional.of(dummyCodeableConceptDocument)).when(repository).findById(any());

    var result = assertDoesNotThrow(() -> codeableConceptService.getSearchResultEntryByCode("1"));

    assertNotNull(result);
    assertEquals(dummyCodeableConceptDocument.termCode(), result);
  }

  @Test
  void testGetSearchResultEntryByCode_throwsOnNotFound() {
    doReturn(Optional.empty()).when(repository).findById(any());

    assertThrows(OntologyItemNotFoundException.class, () -> codeableConceptService.getSearchResultEntryByCode("foo"));
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
    return new SearchHitsImpl<>(totalHits, TotalHitsRelation.OFF, 10.0F, null, null, searchHitsList, null, null, null);
  }

  private CodeableConceptDocument createDummyCodeableConceptDocument(String id) {
    return CodeableConceptDocument.builder()
        .id(id)
        .termCode(createDummyTermcode())
        .valueSets(List.of())
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