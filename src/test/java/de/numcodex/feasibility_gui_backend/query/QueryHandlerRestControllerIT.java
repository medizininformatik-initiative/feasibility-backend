package de.numcodex.feasibility_gui_backend.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.validation.StructuredQueryValidatorSpringConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("query")
@ExtendWith(SpringExtension.class)
@Import({StructuredQueryValidatorSpringConfig.class})
@WebMvcTest(
        controllers = QueryHandlerRestController.class,
        properties = {
                "app.enableQueryValidation=true"
        }
)
@SuppressWarnings("NewClassNamingConvention")
public class QueryHandlerRestControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper jsonUtil;

    @MockBean
    private QueryHandlerService queryHandlerService;

    @Test
    public void testRunQueryEndpoint_FailsOnInvalidStructuredQueryWith400() throws Exception {
        var testQuery = new StructuredQuery();

        mockMvc.perform(post(URI.create("/api/v1/query-handler/run-query"))
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(testQuery)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRunQueryEndpoint_SucceedsOnValidStructuredQueryWith201() throws Exception {
        var termCode = new TermCode();
        termCode.setCode("LL2191-6");
        termCode.setSystem("http://loinc.org");
        termCode.setDisplay("Geschlecht");

        var inclusionCriterion = new Criterion();
        inclusionCriterion.setTermCodes(new ArrayList<>(List.of(termCode)));

        var testQuery = new StructuredQuery();
        testQuery.setInclusionCriteria(List.of(List.of(inclusionCriterion)));
        testQuery.setExclusionCriteria(List.of());
        testQuery.setDisplay("foo");
        testQuery.setVersion(URI.create("http://to_be_decided.com/draft-2/schema#"));

        when(queryHandlerService.runQuery(any(StructuredQuery.class))).thenReturn(1L);

        mockMvc.perform(post(URI.create("/api/v1/query-handler/run-query"))
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(testQuery)))
                .andExpect(status().isCreated())
                .andExpect(header().string(LOCATION, "/api/v1/query-handler/1/result"));
    }
}
