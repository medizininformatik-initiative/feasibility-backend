package de.numcodex.feasibility_gui_backend.query.collect;

import de.numcodex.feasibility_gui_backend.query.persistence.QueryRepository;
import de.numcodex.feasibility_gui_backend.query.persistence.ResultRepository;
import de.numcodex.feasibility_gui_backend.query.persistence.SiteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryCollectSpringConfig {

    @Bean
    public QueryStatusListener createQueryStatusListener(QueryRepository queryRepository,
                                                         SiteRepository siteRepository,
                                                         ResultRepository resultRepository) {
        return new QueryStatusListenerImpl(queryRepository, siteRepository, resultRepository);
    }
}
