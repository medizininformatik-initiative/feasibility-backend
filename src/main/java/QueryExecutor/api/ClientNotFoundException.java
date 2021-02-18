package QueryExecutor.api;

/**
 * Indicates that a client was not found with regards to a specific query.
 */
public class ClientNotFoundException extends Exception {
    public ClientNotFoundException(String queryId, String clientId) {
        super("Client with ID '" + clientId + "' was not found with regards to query with ID '" + queryId + "'.");
    }
}
