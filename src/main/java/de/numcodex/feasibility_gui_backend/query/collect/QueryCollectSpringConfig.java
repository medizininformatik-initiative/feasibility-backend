package de.numcodex.feasibility_gui_backend.query.collect;

import de.numcodex.feasibility_gui_backend.query.persistence.QueryRepository;
import de.numcodex.feasibility_gui_backend.query.result.ResultService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryCollectSpringConfig {

    @Bean
    public QueryStatusListener createQueryStatusListener(QueryRepository queryRepository,
                                                         ResultService resultService) {
        return new QueryStatusListenerImpl(queryRepository, resultService);
    }
}
