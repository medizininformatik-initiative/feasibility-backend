package de.numcodex.feasibility_gui_backend.query.result;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ResultServiceSpringConfig {

  @Bean
  public ResultService createResultService(
      @Value("${app.queryResultExpiryMinutes}") int resultExpiry) {
    log.info("Create ResultService with result TTL of {} minutes", resultExpiry);
    return new ResultService(Duration.ofMinutes(resultExpiry));
  }
}
