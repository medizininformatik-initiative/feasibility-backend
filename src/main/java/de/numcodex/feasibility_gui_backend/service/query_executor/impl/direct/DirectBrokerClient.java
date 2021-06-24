package de.numcodex.feasibility_gui_backend.service.query_executor.impl.direct;

import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatus;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

public class DirectBrokerClient implements BrokerClient {

  private static final String SITE_1_ID ="1";
  private static final String SITE_1_NAME ="FHIR Server";

  @Autowired
  private DirectConnector directConnector;

  private final List<QueryStatusListener> listeners = new ArrayList<>();
  private final List<DirectQuery> queries = new ArrayList<>();

  @Override
  public void addQueryStatusListener(QueryStatusListener queryStatusListener) {
    this.listeners.add(queryStatusListener);
  }

  @Override
  public String createQuery() {
    var query = new DirectQuery();
    this.queries.add(query);

    return query.getQueryId();
  }

  @Override
  public void addQueryDefinition(String queryId, String mediaTypeString, String content)
          throws QueryNotFoundException {
    DirectQuery query = findQuery(queryId);
    query.getContents().put(mediaTypeString, content);
  }

  @Override
  public void publishQuery(String queryId) throws QueryNotFoundException {
    var query = findQuery(queryId);

    int resp = Integer.valueOf(directConnector.getQueryResult(query.getContents().get("application/sq+json")));
    query.getResults().put(SITE_1_ID,resp);
    this.listeners.forEach(listener -> listener.onClientUpdate(query.getQueryId(), SITE_1_ID, QueryStatus.COMPLETED));
  }

  @Override
  public void closeQuery(String queryId) {
    //TODO: NOP
  }

  @Override
  public int getResultFeasibility(String queryId, String siteId)
          throws QueryNotFoundException {
    var query = findQuery(queryId);

    return query.getResults().getOrDefault(siteId, -2);
  }

  @Override
  public List<String> getResultSiteIds(String queryId) throws QueryNotFoundException {
    var query = findQuery(queryId);

    return new ArrayList<>(query.getResults().keySet());
  }

  @Override
  public String getSiteName(String siteId) {
    return switch (siteId) {
      case SITE_1_ID -> SITE_1_NAME;
      default -> "";
    };
  }

  private DirectQuery findQuery(String queryId) throws QueryNotFoundException {
    var queryOptional = this.queries.stream()
            .filter(queryTemp -> queryTemp.getQueryId().equals(queryId)).findFirst();
    if (queryOptional.isEmpty()) {
      throw new QueryNotFoundException(queryId);
    }

    return queryOptional.get();
  }

  @Data
  static class DirectQuery {
    private String queryId = UUID.randomUUID().toString();

    private Map<String, String> contents = new HashMap<>();
    private Map<String, Integer> results = new HashMap<>();
  }
}
