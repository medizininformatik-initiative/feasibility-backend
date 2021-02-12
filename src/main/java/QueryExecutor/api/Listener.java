package QueryExecutor.api;

public interface Listener {

  void onClientUpdate(String queryId, String clientId, QueryStatus status);
}
