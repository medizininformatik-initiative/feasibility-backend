package de.numcodex.feasibility_gui_backend.service.query_executor;

import de.numcodex.feasibility_gui_backend.model.db.Result;
import de.numcodex.feasibility_gui_backend.repository.QueryRepository;
import de.numcodex.feasibility_gui_backend.repository.ResultRepository;
import de.numcodex.feasibility_gui_backend.repository.SiteRepository;

import java.io.IOException;

public class QueryStatusListenerImpl implements QueryStatusListener {

  private final ResultRepository resultRepository;
  private final QueryRepository queryRepository;
  private final SiteRepository siteRepository;
  private final BrokerClient brokerClient;

  public QueryStatusListenerImpl(ResultRepository resultRepository, QueryRepository queryRepository,
      SiteRepository siteRepository, BrokerClient brokerClient) {
    this.resultRepository = resultRepository;
    this.queryRepository = queryRepository;
    this.siteRepository = siteRepository;
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

    var site = siteRepository.findBySiteId(siteId);

    // TODO: proper error handling
    var query = queryRepository.findById(queryId);

    if (query.isPresent()) {
      result.setResult(numberOfPatients);
      result.setQuery(query.get());
      result.setSite(site);

      this.resultRepository.save(result);
    } else {
      System.out.println("No query entity found.");
    }
  }
}
