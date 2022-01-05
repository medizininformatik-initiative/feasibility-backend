package de.numcodex.feasibility_gui_backend.query.broker.direct;

import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring configuration for providing a {@link DirectBrokerClient} instance.
 */
@Lazy
@Configuration
public class DirectSpringConfig {

    @Value("${app.flare.baseUrl}")
    private String flareBaseUrl;

    @Qualifier("direct")
    @Bean
    public BrokerClient directBrokerClient(WebClient directWebClient) {
        return new DirectBrokerClient(directWebClient);
    }

    @Bean
    public WebClient directWebClient() {
        return WebClient.create(flareBaseUrl);
    }
}
