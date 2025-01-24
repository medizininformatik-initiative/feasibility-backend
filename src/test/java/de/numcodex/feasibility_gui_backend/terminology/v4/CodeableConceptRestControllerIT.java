package de.numcodex.feasibility_gui_backend.terminology.v4;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.dse.api.LocalizedValue;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingInterceptor;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.terminology.api.CcSearchResult;
import de.numcodex.feasibility_gui_backend.terminology.api.CodeableConceptEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.CodeableConceptService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.PATH_API;
import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.PATH_CODEABLE_CONCEPT;
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
    controllers = CodeableConceptRestController.class
)
class CodeableConceptRestControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper jsonUtil;

  @MockitoBean
  private CodeableConceptService codeableConceptService;

  @MockitoBean
  private RateLimitingInterceptor rateLimitingInterceptor;

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void testSearchOntologyItemsCriteriaQuery_succeedsWith200() throws Exception {
    CcSearchResult dummyCcSearchResult = createDummyCcSearchResult();
    doReturn(dummyCcSearchResult).when(codeableConceptService).performCodeableConceptSearchWithRepoAndPaging(any(String.class), isNull(), anyInt(), anyInt());

    mockMvc.perform(get(URI.create(PATH_API + PATH_CODEABLE_CONCEPT + "/entry/search"))
            .param("searchterm", "foo")
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalHits").value(dummyCcSearchResult.getTotalHits()))
        .andExpect(jsonPath("$.results.length()").value(dummyCcSearchResult.getResults().size()))
        .andExpect(jsonPath("$.results[0].termCode.code").value(dummyCcSearchResult.getResults().get(0).termCode().code()))
        .andExpect(jsonPath("$.results[0].termCode.system").value(dummyCcSearchResult.getResults().get(0).termCode().system()))
        .andExpect(jsonPath("$.results[0].termCode.version").value(dummyCcSearchResult.getResults().get(0).termCode().version()))
        .andExpect(jsonPath("$.results[0].termCode.display").value(dummyCcSearchResult.getResults().get(0).termCode().display()));
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void testGetCodeableConceptByCode_succeedsWith200() throws Exception {
    var id = UUID.randomUUID();
    List<CodeableConceptEntry> dummyCodeableConceptEntries = createDummyCodeableConceptEntries(List.of(id));
    doReturn(dummyCodeableConceptEntries).when(codeableConceptService).getSearchResultsEntryByIds(anyList());

    mockMvc.perform(get(URI.create(PATH_API + PATH_CODEABLE_CONCEPT + "/entry")).param("ids", id.toString()).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.[0].termCode.code").value(dummyCodeableConceptEntries.get(0).termCode().code()))
        .andExpect(jsonPath("$.[0].termCode.system").value(dummyCodeableConceptEntries.get(0).termCode().system()))
        .andExpect(jsonPath("$.[0].termCode.version").value(dummyCodeableConceptEntries.get(0).termCode().version()))
        .andExpect(jsonPath("$.[0].termCode.display").value(dummyCodeableConceptEntries.get(0).termCode().display()));
  }

  private CcSearchResult createDummyCcSearchResult() {
    var id = UUID.randomUUID();
    return CcSearchResult.builder()
        .totalHits(1)
        .results(createDummyCodeableConceptEntries(List.of(id)))
        .build();
  }

  private List<CodeableConceptEntry> createDummyCodeableConceptEntries(List<UUID> ids) {
    List<CodeableConceptEntry> dummyCodeableConceptEntries = new ArrayList<>();
    for (UUID id : ids) {
      dummyCodeableConceptEntries.add(
          CodeableConceptEntry.builder()
              .id(id.toString())
              .termCode(createDummyTermcode())
              .display(createDummyDisplayEntry())
              .build()
      );
    }
    return dummyCodeableConceptEntries;
  }

  private DisplayEntry createDummyDisplayEntry() {
    return DisplayEntry.builder()
        .original("Code 1")
        .translations(List.of(
            LocalizedValue.builder()
                .value("code 1")
                .language("de-DE")
                .build(),
            LocalizedValue.builder()
                .value("code 1")
                .language("en-US")
                .build()
        ))
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