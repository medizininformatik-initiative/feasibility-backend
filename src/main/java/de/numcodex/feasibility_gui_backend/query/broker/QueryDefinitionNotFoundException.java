package de.numcodex.feasibility_gui_backend.query.broker;

/**
 * Indicates that a query does not contain the necessary query definition.
 */
public class QueryDefinitionNotFoundException extends Exception {

    public QueryDefinitionNotFoundException(String queryId, String queryMediaType) {
        super("Query with ID '" + queryId
            + "' does not contain a query definition for the mandatory type: " + queryMediaType);
    }
}
