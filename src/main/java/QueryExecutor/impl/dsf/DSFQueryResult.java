package QueryExecutor.impl.dsf;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Holds query result information.
 *
 * The information is related to a specific query (queryId) and an organization that the result is coming from (clientId).
 */
@Data
@AllArgsConstructor
class DSFQueryResult {
    private final String queryId;
    private final String clientId;
    private final int measureCount;
}
