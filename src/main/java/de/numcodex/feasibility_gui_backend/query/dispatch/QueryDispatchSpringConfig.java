package de.numcodex.feasibility_gui_backend.query.dispatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import de.numcodex.feasibility_gui_backend.query.persistence.QueryContentRepository;
import de.numcodex.feasibility_gui_backend.query.persistence.QueryDispatchRepository;
import de.numcodex.feasibility_gui_backend.query.persistence.QueryRepository;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslationComponent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Configuration
public class QueryDispatchSpringConfig {

    @Bean
    public QueryDispatcher createQueryDispatcher(
            @Qualifier("brokerClients") List<BrokerClient> brokerClients,
            QueryTranslationComponent queryTranslationComponent,
            QueryHashCalculator queryHashCalculator,
            @Qualifier("translation") ObjectMapper jsonUtil,
            QueryRepository queryRepository,
            QueryContentRepository queryContentRepository,
            QueryDispatchRepository queryDispatchRepository) {
        return new QueryDispatcher(brokerClients, queryTranslationComponent, queryHashCalculator, jsonUtil,
                queryRepository, queryContentRepository, queryDispatchRepository);
    }

    @Bean
    public QueryHashCalculator createQueryHashCalculator() throws NoSuchAlgorithmException {
        return new QueryHashCalculator(MessageDigest.getInstance("SHA3-256"));
    }
}
