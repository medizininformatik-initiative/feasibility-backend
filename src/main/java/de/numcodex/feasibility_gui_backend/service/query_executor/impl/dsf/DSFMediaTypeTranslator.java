package de.numcodex.feasibility_gui_backend.service.query_executor.impl.dsf;

import de.numcodex.feasibility_gui_backend.service.query_executor.UnsupportedMediaTypeException;

import java.util.List;

import static de.numcodex.feasibility_gui_backend.service.QueryMediaTypes.CQL;
import static de.numcodex.feasibility_gui_backend.service.QueryMediaTypes.FHIR;
import static de.numcodex.feasibility_gui_backend.service.QueryMediaTypes.STRUCTURED_QUERY;

/**
 * Utility for translating backend defined query media types into their corresponding DSF middleware counterpart.
 */
class DSFMediaTypeTranslator {
    /**
     * Given a string representation of a backend defined {@link de.numcodex.feasibility_gui_backend.service.QueryMediaTypes}
     * translates it into a media type compatible with DSF middleware instances.
     *
     * @param mediaType The media type that shall be translated.
     * @return The translated media type.
     * @throws UnsupportedMediaTypeException If the given media type is not supported.
     */
    public String translate(String mediaType) throws UnsupportedMediaTypeException {
        return switch (mediaType.toLowerCase()) {
            case STRUCTURED_QUERY -> "application/json";
            case CQL -> CQL;
            case FHIR -> "application/x-fhir-query";
            default -> throw new UnsupportedMediaTypeException(mediaType, List.of(STRUCTURED_QUERY, CQL, FHIR));
        };
    }
}
