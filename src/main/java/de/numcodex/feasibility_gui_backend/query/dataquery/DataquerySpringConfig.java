package de.numcodex.feasibility_gui_backend.query.dataquery;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.persistence.DataqueryRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataquerySpringConfig {

  @Bean
  public DataqueryHandler createDataqueryHandler(
      @Qualifier("translation") ObjectMapper jsonUtil,
      DataqueryRepository dataqueryRepository,
      DataqueryCsvExportService dataqueryCsvExportService,
      @Value("${app.maxSavedQueriesPerUser}") Integer maxSavedQueriesPerUser,
      @Value("${app.keycloakAdminRole}") String keycloakAdminRole
  ) {
    return new DataqueryHandler(jsonUtil, dataqueryRepository, dataqueryCsvExportService, maxSavedQueriesPerUser, keycloakAdminRole);
  }
}
