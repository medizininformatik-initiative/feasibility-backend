package de.numcodex.feasibility_gui_backend.service;

/**
 * Holds information of media types regarding different query types supported by this application.
 */
public final class QueryMediaTypes {

    /**
     * Represents the media type of a structured query.
     */
    public static final String STRUCTURED_QUERY = "application/sq+json";

    /**
     * Represents the media type of a CQL query.
     */
    public static final String CQL = "text/cql";

    /**
     * Represents the media type of a FHIR search query.
     */
    public static final String FHIR = "text/fhir-codex";
}
