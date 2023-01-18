package de.numcodex.feasibility_gui_backend.query.broker.direct;

import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.*;

import static de.numcodex.feasibility_gui_backend.query.QueryMediaType.STRUCTURED_QUERY;
import static de.numcodex.feasibility_gui_backend.query.collect.QueryStatus.COMPLETED;
import static de.numcodex.feasibility_gui_backend.query.collect.QueryStatus.FAILED;

/**
 * A {@link BrokerClient} to be used to directly communicate with a Flare instance without the need
 * for using any middleware (Aktin or DSF).
 */
@Slf4j
public class DirectBrokerClientFlare extends DirectBrokerClient {

  private static final String SITE_1_NAME = "FHIR Server";
  private static final String FLARE_QUERY_ENDPOINT_URL = "/query/execute";
  private final WebClient webClient;

  /**
   * Creates a new {@link DirectBrokerClientFlare} instance that uses the given web client to communicate with a Flare
   * instance.
   *
   * @param webClient A web client to communicate with a Flare instance.
   */
  public DirectBrokerClientFlare(WebClient webClient) {
    this.webClient = Objects.requireNonNull(webClient);
    listeners = new ArrayList<>();
    brokerQueries = new HashMap<>();
    brokerToBackendQueryIdMapping = new HashMap<>();
  }

  @Override
  public void publishQuery(String brokerQueryId) throws QueryNotFoundException, IOException {
    var query = findQuery(brokerQueryId);
    var structuredQueryContent = Optional.ofNullable(query.getQueryDefinition(STRUCTURED_QUERY))
        .orElseThrow(() -> new IllegalStateException("Query with ID "
            + brokerQueryId
            + " does not contain a query definition for the mandatory type: "
            + STRUCTURED_QUERY
        ));

    try {
      webClient.post()
          .uri(FLARE_QUERY_ENDPOINT_URL)
          .header(HttpHeaders.CONTENT_TYPE, STRUCTURED_QUERY.getRepresentation())
          .bodyValue(structuredQueryContent)
          .retrieve()
          .bodyToMono(String.class)
          .map(Integer::valueOf)
          .doOnError(error -> {
            log.error(error.getMessage(), error);
            updateQueryStatus(brokerQueryId, FAILED);
          })
          .subscribe(val -> {
            query.registerSiteResults(SITE_1_ID, obfuscateResultCount ? obfuscate(val) : val);
            updateQueryStatus(brokerQueryId, COMPLETED);
          });
    } catch (Exception e) {
      throw new IOException("An error occurred while publishing the query with ID: " + brokerQueryId, e);
    }
  }

  @Override
  public String getSiteName(String siteId) {
    return siteId.equals(SITE_1_ID) ? SITE_1_NAME : "";
  }

}
