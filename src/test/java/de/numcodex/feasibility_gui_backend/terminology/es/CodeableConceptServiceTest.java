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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.util.List;
import java.util.Optional;

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
    Page<CodeableConceptDocument> dummyCodeableConceptDocumentPage = createDummyCodeableConceptDocumentPage();
    doReturn(dummyCodeableConceptDocumentPage).when(repository).findByNameOrTermcodeMultiMatch0Filters(any(String.class), any(Pageable.class));

    var result = assertDoesNotThrow(() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("foo", List.of(), 20, 0));

    assertNotNull(result);
    assertEquals(dummyCodeableConceptDocumentPage.getTotalElements(), result.getTotalHits());
    assertEquals(dummyCodeableConceptDocumentPage.get().toList().get(0).termCode(), result.getResults().get(0));
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_succeedsWithValueSetFilter() {
    Page<CodeableConceptDocument> dummyCodeableConceptDocumentPage = createDummyCodeableConceptDocumentPage();
    doReturn(dummyCodeableConceptDocumentPage).when(repository).findByNameOrTermcodeMultiMatch1Filter(any(String.class), any(String.class), anyList(), any(Pageable.class));

    var result = assertDoesNotThrow(() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("foo", List.of("bar"), 20, 0));

    assertNotNull(result);
    assertEquals(dummyCodeableConceptDocumentPage.getTotalElements(), result.getTotalHits());
    assertEquals(dummyCodeableConceptDocumentPage.get().toList().get(0).termCode(), result.getResults().get(0));
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_succeedsWithEmptyResult() {
    doReturn(new PageImpl<CodeableConceptDocument>(List.of())).when(repository).findByNameOrTermcodeMultiMatch0Filters(any(String.class), any(Pageable.class));

    var result = assertDoesNotThrow(() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("foo", List.of(), 20, 0));

    assertNotNull(result);
    assertEquals(0, result.getTotalHits());
  }

  @Test
  void testGetSearchResultEntryByCode_succeeds() {
    CodeableConceptDocument dummyCodeableConceptDocument = createDummyCodeableConceptDocument();
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

  private Page<CodeableConceptDocument> createDummyCodeableConceptDocumentPage() {
    return new PageImpl<>(List.of(createDummyCodeableConceptDocument()));
  }

  private CodeableConceptDocument createDummyCodeableConceptDocument() {
    return CodeableConceptDocument.builder()
        .id("1")
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