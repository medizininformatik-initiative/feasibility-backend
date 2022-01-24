package de.numcodex.feasibility_gui_backend.query.translation;

import de.numcodex.feasibility_gui_backend.query.QueryMediaType;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Provides functions for translating {@link StructuredQuery} into different formats.
 */
@RequiredArgsConstructor
public class QueryTranslationComponent {

    @NonNull
    private final Map<QueryMediaType, QueryTranslator> translators;

    /**
     * Translates a {@link StructuredQuery} into different formats using the configured translators.
     *
     * @param query The query that shall be translated.
     * @return The query translated into different formats mapped to their corresponding media type.
     * @throws QueryTranslationException If any translation fails.
     */
    public Map<QueryMediaType, String> translate(StructuredQuery query) throws QueryTranslationException {
        var translationResults = new HashMap<QueryMediaType, String>();
        for (Entry<QueryMediaType, QueryTranslator> translatorMapping : translators.entrySet()) {
            translationResults.put(translatorMapping.getKey(), translatorMapping.getValue().translate(query));
        }
        return translationResults;
    }
}
