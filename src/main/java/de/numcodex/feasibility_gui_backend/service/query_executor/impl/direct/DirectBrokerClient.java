package de.numcodex.feasibility_gui_backend.service.query_executor.impl.direct;

import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

public class DirectBrokerClient implements BrokerClient {

  private static final String SITE_1_NAME ="Lübeck";
  private static final String SITE_2_NAME ="Erlangen";
  private static final String SITE_3_NAME ="Frankfurt";
  private static final String SITE_4_NAME ="Leipzig";

  private static final String SITE_1_ID ="1";
  private static final String SITE_2_ID ="2";
  private static final String SITE_3_ID ="3";
  private static final String SITE_4_ID ="4";


  @Autowired
  private DirectConnector directConnector;

  private final List<QueryStatusListener> listeners = new ArrayList<>();
  private final List<DirectQuery> queries = new ArrayList<>();
  // TODO: Thread handling should be refactored using Executor, Runnable ThreadPool
  private final List<DirectResultThread> runningResultThreads = new ArrayList<>();

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

    runResultThread(SITE_1_ID, query);
    runResultThread(SITE_2_ID, query);
    runResultThread(SITE_3_ID, query);
    runResultThread(SITE_4_ID, query);
  }

  @Override
  public void closeQuery(String queryId) {
    var threadsToBeStopped = this.runningResultThreads.stream()
            .filter(thread -> thread.getQuery().getQueryId().equals(queryId))
            .collect(Collectors.toList());
    threadsToBeStopped.forEach(DirectResultThread::stopMockThread);

    this.runningResultThreads.removeAll(threadsToBeStopped);
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
      case SITE_2_ID -> SITE_2_NAME;
      case SITE_3_ID -> SITE_3_NAME;
      case SITE_4_ID -> SITE_4_NAME;
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

  private void runResultThread(String siteId, DirectQuery query) {
    var thread = new DirectResultThread(siteId, query, listeners, directConnector);
    thread.start();
    this.runningResultThreads.add(thread);
  }

  @Data
  static class DirectQuery {
    private String queryId = UUID.randomUUID().toString();

    private Map<String, String> contents = new HashMap<>();
    private Map<String, Integer> results = new HashMap<>();
  }
}
