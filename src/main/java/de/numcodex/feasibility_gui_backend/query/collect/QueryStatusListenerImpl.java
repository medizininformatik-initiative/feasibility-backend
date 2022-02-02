package de.numcodex.feasibility_gui_backend.query.collect;

import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.SiteNotFoundException;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static de.numcodex.feasibility_gui_backend.query.collect.QueryStatus.COMPLETED;
import static de.numcodex.feasibility_gui_backend.query.collect.QueryStatus.FAILED;
import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.ERROR;
import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.SUCCESS;

/**
 * An entity capable of reacting on updates from broker clients regarding different kinds of query status updates.
 */
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QueryStatusListenerImpl implements QueryStatusListener {

    @NonNull
    private final QueryRepository queryRepository;

    @NonNull
    private final SiteRepository siteRepository;

    @NonNull
    private final ResultRepository resultRepository;

    @Override
    public void onClientUpdate(Long backendQueryId, QueryStatusUpdate statusUpdate) {
        logQueryStatusChange(statusUpdate.brokerQueryId(), statusUpdate.brokerSiteId(),
                statusUpdate.source().getBrokerType(), statusUpdate.status());

        try {
            Optional<Integer> matchesInPopulation = (statusUpdate.status() == COMPLETED)
                    ? Optional.of(resolveMatchesInPopulation(statusUpdate.brokerQueryId(), statusUpdate.brokerSiteId(), statusUpdate.source()))
                    : Optional.empty();

            if (statusUpdate.status() == COMPLETED || statusUpdate.status() == FAILED) {
                var internalQuery = lookupAssociatedBackendQuery(backendQueryId);
                var siteName = resolveSiteName(statusUpdate.brokerSiteId(), statusUpdate.source());
                var site = lookupSite(siteName)
                        .orElseGet(() -> createSite(siteName));

                persistResult(internalQuery, site, matchesInPopulation.orElse(null));
            }
        } catch (QueryResultCollectException e) {
            log.error("cannot persist result of query '%s' for site '%s' with status '%s'".formatted(
                    statusUpdate.brokerQueryId(), statusUpdate.brokerSiteId(), statusUpdate.status().toString()), e);
        }
    }

    private void logQueryStatusChange(String externalQueryId, String siteId, BrokerClientType brokerClientType,
                                      QueryStatus status) {
        log.info("query '%s' of broker client with type '%s' to site '%s' changed its status to '%s'".formatted(
                externalQueryId, brokerClientType.toString(), siteId, status.toString()
        ));
    }

    private int resolveMatchesInPopulation(String externalQueryId, String externalSiteId, BrokerClient client)
            throws QueryResultCollectException {
        try {
            return client.getResultFeasibility(externalQueryId, externalSiteId);
        } catch (QueryNotFoundException | SiteNotFoundException e) {
            throw new QueryResultCollectException("cannot get feasibility result for query '%s' to site '%s' from broker client with type '%s' since query or site are unknown"
                    .formatted(externalQueryId, externalSiteId, client.getBrokerType().toString()), e);
        } catch (IOException e) {
            throw new QueryResultCollectException("cannot get feasibility result for query '%s' to site '%s' from broker client with type '%s'"
                    .formatted(externalQueryId, externalSiteId, client.getBrokerType().toString()), e);
        }
    }

    private Query lookupAssociatedBackendQuery(Long backendQueryId) throws QueryResultCollectException {
        return queryRepository.findById(backendQueryId)
                .orElseThrow(() -> new QueryResultCollectException("cannot find backend query with id '%s'"
                        .formatted(backendQueryId)));
    }

    private String resolveSiteName(String externalSiteId, BrokerClient client) throws QueryResultCollectException {
        try {
            return client.getSiteName(externalSiteId);
        } catch (SiteNotFoundException | IOException e) {
            throw new QueryResultCollectException("cannot resolve site name for site '%s'".formatted(externalSiteId), e);
        }
    }

    private Optional<Site> lookupSite(String siteName) {
        return siteRepository.findBySiteName(siteName);
    }

    private Site createSite(String siteName) {
        var site = new Site();
        site.setSiteName(siteName);
        return siteRepository.save(site);
    }

    private void persistResult(Query internalQuery, Site site, Integer matchesInPopulation)
            throws QueryResultCollectException {
        var result = new Result();
        result.setQuery(internalQuery);
        result.setSite(site);
        result.setResultType((matchesInPopulation != null) ? SUCCESS : ERROR);
        result.setResult(matchesInPopulation);
        result.setReceivedAt(Timestamp.from(Instant.now()));

        try {
            resultRepository.save(result);
        } catch (DataIntegrityViolationException e) {
            // TODO: this should be possible (although as an update) if the former result has had a failed state!
            throw new QueryResultCollectException("result for query '%s' to site '%s' already exists - dropping this one"
                    .formatted(internalQuery.getId(), site.getSiteName()));
        }
    }
}
