package de.numcodex.feasibility_gui_backend.service.query_executor;

/**
 * Indicates that publishing a query failed.
 */
public class PublishFailedException extends Exception {
    public PublishFailedException(String queryId, Throwable e) {
        super("Query with ID '" + queryId + "' could not be published: " + e);
    }
}
