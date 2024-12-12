package de.numcodex.feasibility_gui_backend.query.broker.direct;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import de.numcodex.feasibility_gui_backend.query.broker.OAuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.reactive.function.client.WebClient;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

/**
 * Spring configuration for providing a {@link DirectBrokerClient} implementation instance. Either
 * {@link DirectBrokerClientCql} or {@link DirectBrokerClientFlare}
 */
@Lazy
@Configuration
@Slf4j
public class DirectSpringConfig {

    private final boolean useCql;
    private final String flareBaseUrl;
    private final String cqlBaseUrl;
    private final String username;
    private final String password;
    private final String issuer;
    private final String clientId;
    private final String clientSecret;
    private final int cqlConnectTimeout;
    private final int cqlSocketTimeout;
    private final int cqlConnectionRequestTimeout;

    public DirectSpringConfig(@Value("${app.broker.direct.cql.enabled:false}") boolean useCql,
            @Value("${app.flare.baseUrl}") String flareBaseUrl, @Value("${app.cql.baseUrl}") String cqlBaseUrl,
            @Value("${app.broker.direct.auth.basic.username}") String username,
            @Value("${app.broker.direct.auth.basic.password}") String password,
            @Value("${app.broker.direct.auth.oauth.issuer.url}") String issuer,
            @Value("${app.broker.direct.auth.oauth.client.id}") String clientId,
            @Value("${app.broker.direct.auth.oauth.client.secret}") String clientSecret,
            @Value("${app.broker.direct.cql.timeout.connect}") int cqlConnectTimeout,
            @Value("${app.broker.direct.cql.timeout.socket}") int cqlSocketTimeout,
            @Value("${app.broker.direct.cql.timeout.connectionRequest}") int cqlConnectionRequestTimeout) {
        this.useCql = useCql;
        this.flareBaseUrl = flareBaseUrl;
        this.cqlBaseUrl = cqlBaseUrl;
        this.username = username;
        this.password = password;
        this.issuer = issuer;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.cqlConnectTimeout = cqlConnectTimeout;
        this.cqlSocketTimeout = cqlSocketTimeout;
        this.cqlConnectionRequestTimeout = cqlConnectionRequestTimeout;
    }

    @Qualifier("direct")
    @Bean
    public BrokerClient directBrokerClient(WebClient directWebClientFlare,
                                           @Value("${app.broker.direct.obfuscateResultCount:false}") boolean obfuscateResultCount,
                                           FhirConnector fhirConnector, FhirHelper fhirHelper) {
        if (useCql) {
            log.info("Enable direct broker (type: cql)");
            return new DirectBrokerClientCql(fhirConnector, obfuscateResultCount, fhirHelper);
        } else {
            log.info("Enable direct broker (type: flare)");
            return new DirectBrokerClientFlare(directWebClientFlare, obfuscateResultCount);
        }
    }

    @Bean
    public IGenericClient getFhirClient(FhirContext fhirContext) {
        fhirContext.getRestfulClientFactory().setConnectTimeout(cqlConnectTimeout);
        fhirContext.getRestfulClientFactory().setSocketTimeout(cqlSocketTimeout);
        fhirContext.getRestfulClientFactory().setConnectionRequestTimeout(cqlConnectionRequestTimeout);
        IGenericClient iGenericClient = fhirContext.newRestfulGenericClient(cqlBaseUrl);
        if (!isNullOrEmpty(password) && !isNullOrEmpty(username)) {
            log.info("Configure direct broker instance with basic authentication (type: cql, url: {}, username: {})",
                    cqlBaseUrl,
                    username);
            iGenericClient.registerInterceptor(new BasicAuthInterceptor(username, password));
        } else if (!isNullOrEmpty(issuer) && !isNullOrEmpty(clientId) && !isNullOrEmpty(clientSecret)) {
            log.info("Configure direct broker instance with oauth authentication"
                    + " (type: cql, url: {}, issuer: {}, client-id: {})",
                    cqlBaseUrl, issuer, clientId);
            iGenericClient.registerInterceptor(new OAuthInterceptor(issuer, clientId, clientSecret));
        } else {
            log.info("Configure direct broker instance (type: cql, url: {})", cqlBaseUrl);
        }
        log.info("Direct broker instance timeouts are set to: Connect - {}ms, Socket - {}ms, ConnectionRequest - {}ms",
            cqlConnectTimeout, cqlSocketTimeout, cqlConnectionRequestTimeout);
        return iGenericClient;
    }

    @Bean
    public WebClient directWebClientFlare() {
        if (!isNullOrEmpty(password) && !isNullOrEmpty(username)) {
            log.info("Configure direct broker instance with basic authentication (type: flare, url: {}, username: {})",
                    flareBaseUrl, username);
            return WebClient.builder()
                    .filter(basicAuthentication(username, password))
                    .baseUrl(flareBaseUrl)
                    .build();
        } else {
            log.info("Configure direct broker instance (type: flare, url: {})", flareBaseUrl);
            return WebClient.create(flareBaseUrl);
        }
    }

}
