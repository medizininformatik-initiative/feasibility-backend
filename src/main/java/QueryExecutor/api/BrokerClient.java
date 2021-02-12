package QueryExecutor.api;

import java.util.List;

public interface BrokerClient {

  void addListener(Listener listener);

  String createQuery();

  void addQueryDefinition(String queryId, String mediaType, String content);

  void publishQuery(String queryId);

  void closeQuery(String queryId);

  int getResultFeasibility(String queryId, String clientId);

  List<String> getResultClientIds(String queryId);
}
