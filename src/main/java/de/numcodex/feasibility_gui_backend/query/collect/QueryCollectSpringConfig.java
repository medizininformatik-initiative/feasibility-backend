package de.numcodex.feasibility_gui_backend.query.collect;

import de.numcodex.feasibility_gui_backend.repository.QueryDispatchRepository;
import de.numcodex.feasibility_gui_backend.repository.ResultRepository;
import de.numcodex.feasibility_gui_backend.repository.SiteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryCollectSpringConfig {

    @Bean
    public QueryStatusListener createQueryStatusListener(QueryDispatchRepository queryDispatchRepository,
                                                         SiteRepository siteRepository,
                                                         ResultRepository resultRepository) {
        return new QueryStatusListenerImpl(queryDispatchRepository, siteRepository, resultRepository);
    }
}
