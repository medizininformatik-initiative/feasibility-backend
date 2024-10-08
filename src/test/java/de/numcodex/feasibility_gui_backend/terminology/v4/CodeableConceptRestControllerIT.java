package de.numcodex.feasibility_gui_backend.terminology.v4;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingInterceptor;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.terminology.api.CcSearchResult;
import de.numcodex.feasibility_gui_backend.terminology.es.CodeableConceptService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.List;

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

  @MockBean
  private CodeableConceptService codeableConceptService;

  @MockBean
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
        .andExpect(jsonPath("$.results[0].code").value(dummyCcSearchResult.getResults().get(0).code()))
        .andExpect(jsonPath("$.results[0].system").value(dummyCcSearchResult.getResults().get(0).system()))
        .andExpect(jsonPath("$.results[0].version").value(dummyCcSearchResult.getResults().get(0).version()))
        .andExpect(jsonPath("$.results[0].display").value(dummyCcSearchResult.getResults().get(0).display()));
  }

  @Test
  @WithMockUser(roles = "DATAPORTAL_TEST_USER")
  void testGetCodeableConceptByCode_succeedsWith200() throws Exception {
    TermCode dummyTermcode = createDummyTermcode();
    doReturn(dummyTermcode).when(codeableConceptService).getSearchResultEntryByCode(any(String.class));

    mockMvc.perform(get(URI.create(PATH_API + PATH_CODEABLE_CONCEPT + "/entry/1")).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value(dummyTermcode.code()))
        .andExpect(jsonPath("$.system").value(dummyTermcode.system()))
        .andExpect(jsonPath("$.version").value(dummyTermcode.version()))
        .andExpect(jsonPath("$.display").value(dummyTermcode.display()));
  }

  private CcSearchResult createDummyCcSearchResult() {
    return CcSearchResult.builder()
        .totalHits(1)
        .results(List.of(createDummyTermcode()))
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