package QueryExecutor.api;

/**
 * Indicates that a requested query was not found.
 */
public class QueryNotFoundException extends Exception {
    public QueryNotFoundException(String queryId) {
        super("Query with ID '" + queryId + "' could not be found.");
    }
}
