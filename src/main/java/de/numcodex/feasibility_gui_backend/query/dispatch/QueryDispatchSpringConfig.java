package de.numcodex.feasibility_gui_backend.query.dispatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslationComponent;
import de.numcodex.feasibility_gui_backend.repository.QueryContentRepository;
import de.numcodex.feasibility_gui_backend.repository.QueryRepository;
import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
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
            @Qualifier("applied") List<BrokerClient> brokerClients,
            QueryTranslationComponent queryTranslationComponent,
            QueryHashCalculator queryHashCalculator,
            @Qualifier("translation") ObjectMapper jsonUtil,
            QueryRepository queryRepository,
            QueryContentRepository queryContentRepository) {
        return new QueryDispatcher(brokerClients, queryTranslationComponent, queryHashCalculator, jsonUtil,
                queryRepository, queryContentRepository);
    }

    @Bean
    public QueryHashCalculator createQueryHashCalculator() throws NoSuchAlgorithmException {
        return new QueryHashCalculator(MessageDigest.getInstance("SHA3-256"));
    }
}
