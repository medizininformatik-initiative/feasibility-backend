package de.numcodex.feasibility_gui_backend.service.query_executor;

import de.numcodex.feasibility_gui_backend.model.db.Query;
import de.numcodex.feasibility_gui_backend.model.db.Result;
import de.numcodex.feasibility_gui_backend.model.db.Result.ResultId;
import de.numcodex.feasibility_gui_backend.model.db.ResultType;
import de.numcodex.feasibility_gui_backend.model.db.Site;
import de.numcodex.feasibility_gui_backend.repository.QueryRepository;
import de.numcodex.feasibility_gui_backend.repository.ResultRepository;
import de.numcodex.feasibility_gui_backend.repository.SiteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Slf4j
public class QueryStatusListenerImpl implements QueryStatusListener {

  // TODO: we need another table to map external runs identifiers to our query
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

  // TODO: should get a function passed to retrieve the site name (not the siteId directly)
  //       This way it does not matter which broker this result belongs to!
  //       Also cuts off the broker client as a dependency since this lookup is the only action
  //       it is good for!
  @Override
  public void onClientUpdate(String brokerspecificQueryId, String siteId, QueryStatus status) {
    if (status != QueryStatus.COMPLETED) {
      return;
    }

    Result result = new Result();
    int numberOfPatients;
    try {
      numberOfPatients = brokerClient.getResultFeasibility(brokerspecificQueryId, siteId);
    } catch (QueryNotFoundException | SiteNotFoundException | IOException e) {
      System.out.println(e.getMessage());
      return;
    }

    try {
      Optional<Site> site = Optional.empty();
      Optional<Query> query = Optional.empty();
      String siteName;

      // TODO: refactor!
//      switch (brokerClient.getClass().getSimpleName()) {
//        case "DirectBrokerClient":
//          site = siteRepository.findBySiteId(Long.parseLong(siteId));
//          query = queryRepository.findByDirectId(brokerspecificQueryId);
//          break;
//        case "AktinBrokerClient":
//          siteName = brokerClient.getSiteName(siteId);
//          site = siteRepository.findByAktinIdentifier(siteName);
//          query = queryRepository.findByAktinId(brokerspecificQueryId);
//          break;
//        case "DSFBrokerClient":
//          site = siteRepository.findByDsfIdentifier(siteId);
//          query = queryRepository.findByDsfId(brokerspecificQueryId);
//          break;
//        case "MockBrokerClient":
//        default:
//          site = siteRepository.findBySiteId(Long.parseLong(siteId));
//          query = queryRepository.findByMockId(brokerspecificQueryId);
//          break;
//      }

      if (query.isPresent() && site.isPresent()) {
        ResultId resultId = new ResultId();
        resultId.setQueryId(query.get().getId());
        resultId.setSiteId(site.get().getId());
        result.setId(resultId);
        result.setResult(numberOfPatients);
        result.setQuery(query.get());
        result.setSite(site.get());
        result.setResultType(ResultType.SUCCESS);
        result.setDisplaySiteId(getFreeDisplaySiteId(query.get().getId()));
        try {
          this.resultRepository.save(result);
        } catch (DataIntegrityViolationException e) {
          log.warn("Duplicate result received. Omitting. Site=[{}], ResultId=[{}]", site.get().getSiteName(), resultId);
        }
      } else {
        System.out.println("No query or no site entity found.");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Integer getFreeDisplaySiteId(Long queryId) {
    var siteIds = siteRepository.getSiteIds();
    var usedSiteIds = resultRepository.getUsedDisplaySiteIds(queryId);

    siteIds.removeIf(id -> usedSiteIds.contains(id));
    Collections.shuffle(siteIds);
    return siteIds.get(0);
  }
}
