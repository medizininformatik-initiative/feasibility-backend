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
 * Spring configuration for providing a {@link DirectBrokerClientCql} instance.
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
    public BrokerClient directBrokerClient(WebClient directWebClient) {
        if (useCql) {
            FhirContext fhirContext = FhirContext.forR4();
            IGenericClient fhirClient = fhirContext.newRestfulGenericClient(cqlBaseUrl);
            return new DirectBrokerClientCql(fhirContext, fhirClient);
        } else {
            return new DirectBrokerClientFlare(directWebClient);
        }
    }

    @Bean
    public WebClient directWebClient() {
        return WebClient.create(flareBaseUrl);
    }

}
