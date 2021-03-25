package de.numcodex.feasibility_gui_backend.service.query_executor;

import de.numcodex.feasibility_gui_backend.model.db.Result;
import de.numcodex.feasibility_gui_backend.repository.ResultRepository;
import java.io.IOException;

public class QueryStatusListenerImpl implements QueryStatusListener {

  private final ResultRepository resultRepository;
  private final BrokerClient brokerClient;

  public QueryStatusListenerImpl(ResultRepository resultRepository, BrokerClient brokerClient) {
    this.resultRepository = resultRepository;
    this.brokerClient = brokerClient;
  }

  @Override
  public void onClientUpdate(String queryId, String siteId, QueryStatus status) {
    if (status != QueryStatus.COMPLETED) {
      return;
    }

    Result result = new Result();
    int numberOfPatients;
    try {
      numberOfPatients = brokerClient.getResultFeasibility(queryId, siteId);
    } catch (QueryNotFoundException | SiteNotFoundException | IOException e) {
      System.out.println(e.getMessage());
      return;
    }

    result.setQueryId(queryId);
    result.setSiteId(siteId);
    result.setNumberOfPatients(numberOfPatients);

    this.resultRepository.save(result);
  }
}
