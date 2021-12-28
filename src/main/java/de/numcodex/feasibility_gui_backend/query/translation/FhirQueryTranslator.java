package de.numcodex.feasibility_gui_backend.query.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * A translator for translating a {@link StructuredQuery} into its FHIR search query format.
 */
@RequiredArgsConstructor
public class FhirQueryTranslator implements QueryTranslator {

    private static final String FLARE_QUERY_TRANSLATE_ENDPOINT_PATH = "/query-translate";
    private static final String FLARE_QUERY_TRANSLATE_CONTENT_TYPE = "codex/json";
    private static final String FLARE_QUERY_TRANSLATE_ACCEPT = "internal/json";

    // TODO: this one should be replaced with a WebClient instance for asynchronous translation support.
    //       However, this will require changes to the interface as well. Additional changes will propagate
    //       upstream. This would be too big of a change. Because of this and Flare being rewritten and potentially
    //       being present as a library (no client needed anymore), we don't pursue this change.
    @NonNull
    private final RestTemplate client;

    @NonNull
    private final ObjectMapper jsonUtil;

    @Override
    public String translate(StructuredQuery query) throws QueryTranslationException {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.putAll(Map.of(
                HttpHeaders.ACCEPT, List.of(FLARE_QUERY_TRANSLATE_ACCEPT),
                HttpHeaders.CONTENT_TYPE, List.of(FLARE_QUERY_TRANSLATE_CONTENT_TYPE)
        ));

        try {
            HttpEntity<String> request = new HttpEntity<>(jsonUtil.writeValueAsString(query), requestHeaders);
            return client.postForObject(FLARE_QUERY_TRANSLATE_ENDPOINT_PATH, request, String.class);
        } catch (JsonProcessingException e) {
            throw new QueryTranslationException("cannot encode structured query as JSON", e);
        } catch (RestClientException e) {
            throw new QueryTranslationException("cannot translate structured query in FHIR search format using Flare", e);
        }
    }
}
