package de.numcodex.feasibility_gui_backend.service.query_executor.impl.mock;

import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatus;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListener;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: Use interface Runnable instead of extending Thread
class MockResultThread extends Thread {

  private final AtomicBoolean running = new AtomicBoolean(true);

  private final String siteId;
  private final MockBrokerClient.MockQuery query;
  private final List<QueryStatusListener> listeners;

  private QueryStatus status = QueryStatus.COMPLETED;

  public MockResultThread(String siteId, MockBrokerClient.MockQuery query, List<QueryStatusListener> listeners) {
    this.siteId = siteId;
    this.query = query;
    this.listeners = listeners;
  }

  void stopMockThread() {
    this.running.set(false);
    this.status = QueryStatus.FAILED;
  }

  public MockBrokerClient.MockQuery getQuery() {
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

    this.query.getResults().put(siteId, (int) Math.round(10 + 500 * Math.random()));

    this.listeners.forEach(listener -> listener.onClientUpdate(query.getQueryId(), siteId, this.status));
  }
}
