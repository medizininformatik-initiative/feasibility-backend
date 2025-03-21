package de.numcodex.feasibility_gui_backend.query.dataquery;

import de.numcodex.feasibility_gui_backend.query.persistence.DataqueryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataquerySpringConfig {

  @Bean
  public DataqueryHandler createDataqueryHandler(
      DataqueryRepository dataqueryRepository,
      @Value("${app.maxSavedQueriesPerUser}") Integer maxSavedQueriesPerUser
  ) {
    return new DataqueryHandler(dataqueryRepository, maxSavedQueriesPerUser);
  }
}
