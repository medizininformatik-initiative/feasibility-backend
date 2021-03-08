package QueryExecutor.api;

/**
 * Represents different states a query can be in.
 */
public enum QueryStatus {
  RETRIEVED,
  QUEUED,
  EXECUTING,
  COMPLETED,
  FAILED
}
