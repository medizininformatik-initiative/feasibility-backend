package de.numcodex.feasibility_gui_backend.query.broker.direct;

import static de.numcodex.feasibility_gui_backend.query.persistence.BrokerClientType.DIRECT;

import de.numcodex.feasibility_gui_backend.query.QueryMediaType;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.SiteNotFoundException;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatus;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatusUpdate;
import de.numcodex.feasibility_gui_backend.query.persistence.BrokerClientType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

public abstract class DirectBrokerClient implements BrokerClient {

  static final String SITE_1_ID = "1";

  protected List<QueryStatusListener> listeners;
  protected Map<String, DirectQuery> brokerQueries;
  protected Map<String, Long> brokerToBackendQueryIdMapping;

  @Value("${app.broker.direct.obfuscateResultCount:false}")
  protected boolean obfuscateResultCount;

  @Override
  public final BrokerClientType getBrokerType() {
    return DIRECT;
  }

  @Override
  public void addQueryStatusListener(QueryStatusListener queryStatusListener) {
    listeners.add(queryStatusListener);
  }

  @Override
  public String createQuery(Long backendQueryId) {
    var brokerQuery = DirectQuery.create();
    var brokerQueryId = brokerQuery.getQueryId();
    brokerQueries.put(brokerQueryId, brokerQuery);
    brokerToBackendQueryIdMapping.put(brokerQueryId, backendQueryId);

    return brokerQueryId;
  }

  @Override
  public void addQueryDefinition(String brokerQueryId, String mediaType, String content) throws QueryNotFoundException {
    findQuery(brokerQueryId).addQueryDefinition(mediaType, content);
  }

  @Override
  public abstract void publishQuery(String brokerQueryId) throws QueryNotFoundException, IOException;

  @Override
  public void closeQuery(String brokerQueryId) throws QueryNotFoundException {
    if (brokerQueries.remove(brokerQueryId) == null) {
      throw new QueryNotFoundException(brokerQueryId);
    }
  }

  @Override
  public int getResultFeasibility(String brokerQueryId, String siteId) throws QueryNotFoundException, SiteNotFoundException {
    return findQuery(brokerQueryId).getSiteResult(siteId);
  }

  @Override
  public List<String> getResultSiteIds(String brokerQueryId) throws QueryNotFoundException {
    return findQuery(brokerQueryId).getSiteIdsWithResult();
  }

  @Override
  public abstract String getSiteName(String siteId);

  /**
   * Searches for a {@link DirectQuery} within this broker. The specified query ID identifies the
   * query.
   *
   * @param queryId The identifier of the query.
   * @return The query if there is any.
   * @throws QueryNotFoundException If the ID does not identify a known query.
   */
  protected DirectQuery findQuery(String queryId) throws QueryNotFoundException {
    return Optional.ofNullable(brokerQueries.get(queryId))
        .orElseThrow(() -> new QueryNotFoundException(queryId));
  }

  /**
   * "Obfuscates" a number by adding or subtracting a random number <=5.
   * @param resultCount the precise result
   * @return The obfuscated result, or 0 if the obfuscated result is < 5
   */
  protected int obfuscate(int resultCount) {
    int randomNum = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
    int obfuscatedResultCount = resultCount + (randomNum % 11) - 5;
    if (obfuscatedResultCount < 5) {
      return 0;
    } else {
      return obfuscatedResultCount;
    }
  }

  /**
   * Updates a query status in all registered listeners.
   * @param queryId the id of the query to update
   * @param queryStatus the {@link QueryStatus} to publish to the listeners
   */
  protected void updateQueryStatus(String queryId, QueryStatus queryStatus) {
    var statusUpdate = new QueryStatusUpdate(this, queryId, SITE_1_ID, queryStatus);
    var associatedBackendQueryId = brokerToBackendQueryIdMapping.get(queryId);
    listeners.forEach(
        l -> l.onClientUpdate(associatedBackendQueryId, statusUpdate)
    );
  }

  /**
   * Updates a query status in all registered listeners.
   * @param query the query to update
   * @param queryStatus the {@link QueryStatus} to publish to the listeners
   */
  protected void updateQueryStatus(DirectQuery query, QueryStatus queryStatus) {
    updateQueryStatus(query.getQueryId(), queryStatus);
  }

  /**
   * A data container representing a query used for direct communications with a Flare instance.
   */
  public static class DirectQuery {

    @Getter
    private final String queryId;
    private final Map<String, String> queryDefinitions;
    private final Map<String, Integer> resultsBySite;

    private DirectQuery(String queryId) {
      this.queryId = queryId;
      queryDefinitions = new HashMap<>();
      resultsBySite = new HashMap<>();
    }

    /**
     * Creates a new {@link DirectQuery} with a random UUID as a query ID.
     *
     * @return The created query.
     */
    public static DirectQuery create() {
      return new DirectQuery(UUID.randomUUID().toString());
    }

    /**
     * Adds a query definition. A query definition is a query in a specific format (i.e. structured
     * query / CQL). The specified mime type defines the format of the query itself. When invoked
     * multiple times for a single mime type, any already existing query content associated with
     * this mime type gets overwritten.
     * <p>
     * For more information on available mime types see {@link QueryMediaType}.
     *
     * @param mimeType The mime type defining the format of the query.
     * @param content The actual query in its string representation.
     */
    public void addQueryDefinition(String mimeType, String content) {
      queryDefinitions.put(mimeType, content);
    }

    /**
     * Gets a single query definition associated with the given mime type.
     *
     * @param mimeType The mime type (query format) of the query.
     * @return The query in its string representation or null if there is no query definition
     * associated with the specified mime type.
     */
    public String getQueryDefinition(QueryMediaType mimeType) {
      return queryDefinitions.get(mimeType.getRepresentation());
    }

    /**
     * Adds the feasibility result of a single site.
     *
     * @param siteId Identifies the site the feasibility result is linked to.
     * @param feasibility The feasibility result.
     */
    public void registerSiteResults(String siteId, int feasibility) {
      resultsBySite.put(siteId, feasibility);
    }

    /**
     * Gets the feasibility result of a single site.
     *
     * @param siteId Identifies the site in question.
     * @return The feasibility result of the site identified by the specified identifier.
     * @throws SiteNotFoundException If the ID does not identify a known site.
     */
    public int getSiteResult(String siteId) throws SiteNotFoundException {
      return Optional.ofNullable(resultsBySite.get(siteId))
          .orElseThrow(() -> new SiteNotFoundException(queryId, siteId));
    }

    /**
     * Gets all site identifiers which already reported a feasibility result.
     *
     * @return A list of identifiers of sites.
     */
    public List<String> getSiteIdsWithResult() {
      return new ArrayList<>(resultsBySite.keySet());
    }
  }
}
