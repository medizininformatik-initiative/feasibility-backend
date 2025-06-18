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
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

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
    private final Duration timeout;
    private String issuer;
    private String clientId;
    private String clientSecret;

    public DirectSpringConfig(@Value("${app.broker.direct.useCql:false}") boolean useCql,
            @Value("${app.flare.baseUrl:}") String flareBaseUrl, @Value("${app.cql.baseUrl:}") String cqlBaseUrl,
            @Value("${app.broker.direct.auth.basic.username:}") String username,
            @Value("${app.broker.direct.auth.basic.password:}") String password,
            @Value("${app.broker.direct.auth.oauth.issuer.url:}") String issuer,
            @Value("${app.broker.direct.auth.oauth.client.id:}") String clientId,
            @Value("${app.broker.direct.auth.oauth.client.secret:}") String clientSecret,
            @Value("#{T(java.time.Duration).parse('${app.broker.direct.timeout:PT20S}')}") Duration timeout) {
        this.useCql = useCql;
        this.flareBaseUrl = flareBaseUrl;
        this.cqlBaseUrl = cqlBaseUrl;
        this.username = username;
        this.password = password;
        this.issuer = issuer;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.timeout = timeout;
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
        var clientFactory = fhirContext.getRestfulClientFactory();
        clientFactory.setSocketTimeout((int) timeout.toMillis());
        fhirContext.setRestfulClientFactory(clientFactory);
        var client = fhirContext.newRestfulGenericClient(cqlBaseUrl);
        if (!isNullOrEmpty(password) && !isNullOrEmpty(username)) {
            log.info("Configure direct broker instance with basic authentication"
                    + " (type: cql, url: {}, username: {}, timeout: {})",
                    cqlBaseUrl, username, timeout);
            client.registerInterceptor(new BasicAuthInterceptor(username, password));
        } else if (!isNullOrEmpty(issuer) && !isNullOrEmpty(clientId) && !isNullOrEmpty(clientSecret)) {
            log.info("Configure direct broker instance with oauth authentication"
                    + " (type: cql, url: {}, issuer: {}, client-id: {}, timeout: {})",
                    cqlBaseUrl, issuer, clientId, timeout);
            client.registerInterceptor(new OAuthInterceptor(issuer, clientId, clientSecret));
        } else {
            log.info("Configure direct broker instance (type: cql, url: {}, timeout: {})", cqlBaseUrl, timeout);
        }
        return client;
    }

    @Bean
    public WebClient directWebClientFlare() {
        var clientBuilder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(timeout)))
                .baseUrl(flareBaseUrl);

        if (!isNullOrEmpty(password) && !isNullOrEmpty(username)) {
            log.info("Configure direct broker instance with basic authentication"
                    + " (type: flare, url: {}, username: {}, timeout: {})",
                    flareBaseUrl, username, timeout);
            return clientBuilder
                    .filter(basicAuthentication(username, password))
                    .build();
        } else {
            log.info("Configure direct broker instance (type: flare, url: {}, timeout: {})", flareBaseUrl, timeout);
            return clientBuilder.build();
        }
    }

}
