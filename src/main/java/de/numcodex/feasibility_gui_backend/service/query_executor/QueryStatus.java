package de.numcodex.feasibility_gui_backend.service.query_executor;

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
