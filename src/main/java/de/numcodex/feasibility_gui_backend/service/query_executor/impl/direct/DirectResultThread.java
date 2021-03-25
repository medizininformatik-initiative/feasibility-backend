package de.numcodex.feasibility_gui_backend.service.query_executor.impl.direct;

import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatus;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListener;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: Use interface Runnable instead of extending Thread


class DirectResultThread extends Thread {

  private final AtomicBoolean running = new AtomicBoolean(true);



  private DirectConnector directConnector;

  //private final DirectConnector directConnector = new DirectConnector(null,null);

  private final String siteId;
  private final DirectBrokerClient.DirectQuery query;
  private final List<QueryStatusListener> listeners;

  private QueryStatus status = QueryStatus.COMPLETED;

  public DirectResultThread(String siteId, DirectBrokerClient.DirectQuery query, List<QueryStatusListener> listeners, DirectConnector directConnector) {
    this.siteId = siteId;
    this.query = query;
    this.listeners = listeners;
    this.directConnector = directConnector;
  }

  void stopMockThread() {
    this.running.set(false);
    this.status = QueryStatus.FAILED;
  }

  public DirectBrokerClient.DirectQuery getQuery() {
    return query;
  }

  @Override
  public void run() {
    this.running.set(true);
    while (running.get()) {
      try {
        Thread.sleep(Math.round(2000 + 6000 * Math.random()));
        this.running.set(false);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.out.println("Result thread (" + siteId + ") was interrupted");
        stopMockThread();
      }
    }

    //Add connection To flare here for each side (for start same flare for each side)
    int resp = Integer.valueOf(directConnector.getQueryResult(query.getContents().get("text/structured-query")));
    this.query.getResults().put(siteId,resp);

    this.listeners.forEach(listener -> listener.onClientUpdate(query.getQueryId(), siteId, this.status));
  }
}
