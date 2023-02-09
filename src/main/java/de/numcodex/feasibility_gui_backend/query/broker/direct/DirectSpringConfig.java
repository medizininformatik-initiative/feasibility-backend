package de.numcodex.feasibility_gui_backend.query.broker.direct;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring configuration for providing a {@link DirectBrokerClient} implementation instance.
 * Either {@link DirectBrokerClientCql} or {@link DirectBrokerClientFlare}
 */
@Lazy
@Configuration
public class DirectSpringConfig {

    @Value("${app.broker.direct.useCql:false}")
    private boolean useCql;

    @Value("${app.flare.baseUrl}")
    private String flareBaseUrl;

    @Value("${app.cql.baseUrl}")
    private String cqlBaseUrl;

    @Qualifier("direct")
    @Bean
    public BrokerClient directBrokerClient(WebClient directWebClientFlare, @Value("${app.broker.direct.obfuscateResultCount:false}") boolean obfuscateResultCount,
        FhirConnector fhirConnector, FhirHelper fhirHelper) {
        if (useCql) {

            return new DirectBrokerClientCql(fhirConnector, obfuscateResultCount,
                fhirHelper);
        } else {
            return new DirectBrokerClientFlare(directWebClientFlare, obfuscateResultCount);
        }
    }

    @Bean
    public IGenericClient getFhirClient(FhirContext fhirContext){
        return fhirContext.newRestfulGenericClient(cqlBaseUrl);
    }

    @Bean
    public WebClient directWebClientFlare() {
        return WebClient.create(flareBaseUrl);
    }

}
