package de.numcodex.feasibility_gui_backend.query.broker.direct;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

/**
 * Spring configuration for providing a {@link DirectBrokerClient} implementation instance.
 * Either {@link DirectBrokerClientCql} or {@link DirectBrokerClientFlare}
 */
@Lazy
@Configuration
public class DirectSpringConfig {

    private final boolean useCql;

    private final String flareBaseUrl;

    private final String cqlBaseUrl;

    private final String username;

    private final String password;

    public DirectSpringConfig(@Value("${app.broker.direct.useCql:false}") boolean useCql, @Value("${app.flare.baseUrl}") String flareBaseUrl, @Value("${app.cql.baseUrl}") String cqlBaseUrl, @Value("${app.broker.direct.auth.basic.username}") String username, @Value("${app.broker.direct.auth.basic.password}") String password) {
        this.useCql = useCql;
        this.flareBaseUrl = flareBaseUrl;
        this.cqlBaseUrl = cqlBaseUrl;
        this.username = username;
        this.password = password;
    }

    @Qualifier("direct")
    @Bean
    public BrokerClient directBrokerClient(WebClient directWebClientFlare, @Value("${app.broker.direct.obfuscateResultCount:false}") boolean obfuscateResultCount,
        FhirConnector fhirConnector, FhirHelper fhirHelper) {
        if (useCql) {
            return new DirectBrokerClientCql(fhirConnector, obfuscateResultCount, fhirHelper);
        } else {
            return new DirectBrokerClientFlare(directWebClientFlare, obfuscateResultCount);
        }
    }

    @Bean
    public IGenericClient getFhirClient(FhirContext fhirContext) {
        IGenericClient iGenericClient = fhirContext.newRestfulGenericClient(cqlBaseUrl);
        if (username != null && password != null && !username.isEmpty() && !password.isEmpty()) {
            IClientInterceptor authInterceptor = new BasicAuthInterceptor(username, password);
            iGenericClient.registerInterceptor(authInterceptor);
        }
        return iGenericClient;
    }

    @Bean
    public WebClient directWebClientFlare() {
        if (username != null && password != null && !username.isEmpty() && !password.isEmpty()) {
            return WebClient.builder()
                    .filter(basicAuthentication(username, password))
                    .baseUrl(flareBaseUrl)
                    .build();
        } else {
            return WebClient.create(flareBaseUrl);
        }
    }

}
