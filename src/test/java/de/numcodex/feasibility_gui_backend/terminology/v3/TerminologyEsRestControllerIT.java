package de.numcodex.feasibility_gui_backend.terminology.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingInterceptor;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResult;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResultEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.TerminologyEsService;
import de.numcodex.feasibility_gui_backend.terminology.es.model.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("terminology")
@Tag("elasticsearch")
@ExtendWith(SpringExtension.class)
@Import(RateLimitingServiceSpringConfig.class)
@WebMvcTest(
    controllers = TerminologyEsRestController.class
)
class TerminologyEsRestControllerIT {

  private static final Logger log = LoggerFactory.getLogger(TerminologyEsRestControllerIT.class);
  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TerminologyEsService terminologyEsService;

  @MockBean
  private RateLimitingInterceptor rateLimitingInterceptor;

  @Autowired
  private ObjectMapper jsonUtil;

  @Test
  @WithMockUser(roles = "FEASIBILITY_TEST_USER")
  public void testGetFilters_succeeds() throws Exception {
    List<String> filterList = List.of("context", "kdsModule", "terminology");
    List<TermFilter> termFilterList = createTermFilterList(filterList.toArray(new String[0]));
    doReturn(termFilterList).when(terminologyEsService).getAvailableFilters();

    mockMvc.perform(get(URI.create("/api/v3/terminology/search/filter")).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonUtil.writeValueAsString(termFilterList)));
  }

  @Test
  public void testGetFilters_failsOnUnauthorized() throws Exception {
    mockMvc.perform(get(URI.create("/api/v3/terminology/search/filter")).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "FEASIBILITY_TEST_USER")
  public void testSearchOntologyItemsCriteriaQuery_succeeds() throws Exception {
    var totalHits = 1;
    var dummyEsSearchResult = createDummyEsSearchResult(totalHits);
    doReturn(dummyEsSearchResult).when(terminologyEsService).performOntologySearchWithRepoAndPaging(any(String.class), isNull(), isNull(), isNull(), anyBoolean(), anyInt(), anyInt());

    mockMvc.perform(get(URI.create("/api/v3/terminology/entry/search")).param("searchterm", "some-context").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalHits").value(dummyEsSearchResult.totalHits()))
        .andExpect(jsonPath("$.results[0].id").value(dummyEsSearchResult.results().get(0).id()))
        .andExpect(jsonPath("$.results[0].name").value(dummyEsSearchResult.results().get(0).name()))
        .andExpect(jsonPath("$.results[0].terminology").value(dummyEsSearchResult.results().get(0).terminology()))
        .andExpect(jsonPath("$.results[0].selectable").value(dummyEsSearchResult.results().get(0).selectable()))
        .andExpect(jsonPath("$.results[0].kdsModule").value(dummyEsSearchResult.results().get(0).kdsModule()))
        .andExpect(jsonPath("$.results[0].availability").value(dummyEsSearchResult.results().get(0).availability()))
        .andExpect(jsonPath("$.results[0].context").value(dummyEsSearchResult.results().get(0).context()));

  }

  @Test
  public void testSearchOntologyItemsCriteriaQuery_failsOnUnauthorized() throws Exception {
    doReturn(createDummyEsSearchResult(1)).when(terminologyEsService).performOntologySearchWithRepoAndPaging(any(String.class), anyList(), anyList(), anyList(), any(Boolean.class), any(Integer.class), any(Integer.class));

    mockMvc.perform(get(URI.create("/api/v3/terminology/entry/search")).param("searchterm", "some-context").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "FEASIBILITY_TEST_USER")
  public void testGetOntologyItemRelationsByHash_succeeds() throws Exception {
    var dummyOntologyItemRelations = createDummyOntologyItemRelations();
    doReturn(dummyOntologyItemRelations).when(terminologyEsService).getOntologyItemRelationsByHash(any(String.class));

    mockMvc.perform(get(URI.create("/api/v3/terminology/entry/abc/relations")).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.translations[0].lang").value(dummyOntologyItemRelations.translations().stream().toList().get(0).lang()))
        .andExpect(jsonPath("$.translations[0].value").value(dummyOntologyItemRelations.translations().stream().toList().get(0).value()))
        .andExpect(jsonPath("$.children[0].contextualizedTermcodeHash").value(dummyOntologyItemRelations.children().stream().toList().get(0).contextualizedTermcodeHash()))
        .andExpect(jsonPath("$.children[0].name").value(dummyOntologyItemRelations.children().stream().toList().get(0).name()))
        .andExpect(jsonPath("$.parents[0].contextualizedTermcodeHash").value(dummyOntologyItemRelations.parents().stream().toList().get(0).contextualizedTermcodeHash()))
        .andExpect(jsonPath("$.parents[0].name").value(dummyOntologyItemRelations.parents().stream().toList().get(0).name()))
        .andExpect(jsonPath("$.relatedTerms[0].contextualizedTermcodeHash").value(dummyOntologyItemRelations.relatedTerms().stream().toList().get(0).contextualizedTermcodeHash()))
        .andExpect(jsonPath("$.relatedTerms[0].name").value(dummyOntologyItemRelations.relatedTerms().stream().toList().get(0).name()));
  }

  @Test
  public void testGetOntologyItemRelationsByHash_failsOnUnauthorized() throws Exception {
    var dummyOntologyItemRelations = createDummyOntologyItemRelations();
    doReturn(dummyOntologyItemRelations).when(terminologyEsService).getOntologyItemRelationsByHash(any(String.class));

    mockMvc.perform(get(URI.create("/api/v3/terminology/entry/abc/relations")).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "FEASIBILITY_TEST_USER")
  public void testGetOntologyItemByHash_succeeds() throws Exception {
    var dummySearchResultEntry = createDummyEsSearchResultEntry();
    doReturn(dummySearchResultEntry).when(terminologyEsService).getSearchResultEntryByHash(any(String.class));

    mockMvc.perform(get(URI.create("/api/v3/terminology/entry/abc")).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(dummySearchResultEntry.id()))
        .andExpect(jsonPath("$.name").value(dummySearchResultEntry.name()))
        .andExpect(jsonPath("$.availability").value(dummySearchResultEntry.availability()))
        .andExpect(jsonPath("$.context").value(dummySearchResultEntry.context()))
        .andExpect(jsonPath("$.terminology").value(dummySearchResultEntry.terminology()))
        .andExpect(jsonPath("$.termcode").value(dummySearchResultEntry.termcode()))
        .andExpect(jsonPath("$.kdsModule").value(dummySearchResultEntry.kdsModule()))
        .andExpect(jsonPath("$.selectable").value(dummySearchResultEntry.selectable()));
  }

  @Test
  public void testGetOntologyItemByHash_failsOnUnauthorized() throws Exception {
    var dummySearchResultEntry = createDummyEsSearchResultEntry();
    doReturn(dummySearchResultEntry).when(terminologyEsService).getSearchResultEntryByHash(any(String.class));

    mockMvc.perform(get(URI.create("/api/v3/terminology/entry/abc")).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  private EsSearchResult createDummyEsSearchResult(int totalHits) {
    return EsSearchResult.builder()
        .totalHits(totalHits)
        .results(List.of(createDummyEsSearchResultEntry()))
        .build();
  }

  private EsSearchResultEntry createDummyEsSearchResultEntry() {
    return EsSearchResultEntry.builder()
        .terminology("some-terminology")
        .availability(100)
        .context("some-context")
        .id("abc-123")
        .kdsModule("some-module")
        .name("some-name")
        .selectable(true)
        .build();
  }

  private OntologyItemRelationsDocument createDummyOntologyItemRelations() {
    return OntologyItemRelationsDocument.builder()
        .relatedTerms(List.of(createDummyRelative()))
        .translations(List.of(createDummyTranslation()))
        .parents(List.of(createDummyRelative()))
        .children(List.of(createDummyRelative()))
        .build();
  }

  private Translation createDummyTranslation() {
    return Translation.builder()
        .lang("de")
        .value("Lorem Ipsum")
        .build();
  }

  private Relative createDummyRelative() {
    return Relative.builder()
        .contextualizedTermcodeHash(UUID.randomUUID().toString())
        .name("some-random-name")
        .build();
  }

  private List<TermFilter> createTermFilterList(String[] values) {
    var termFilters = new ArrayList<TermFilter>();
    for (String term : values) {
      termFilters.add(
          TermFilter.builder()
              .name(term)
              .type("selectable-concept")
              .values(List.of(TermFilterValue.builder().label("baz").count(values.length).build()))
              .build()
      );
    }
    termFilters.add(TermFilter.builder()
        .type("boolean")
        .name("availability")
        .values(List.of())
        .build());
    return termFilters;
  }
}
