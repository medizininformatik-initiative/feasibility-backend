package de.numcodex.feasibility_gui_backend.query.ratelimiting;

import de.numcodex.feasibility_gui_backend.query.result.ResultService;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RateLimitingServiceSpringConfig {

  @Bean
  public RateLimitingService createRateLimitingService(
      @Value("${app.privacy.quota.read.any.pollingIntervalSeconds}") int pollingIntervalSeconds) {
    log.info("Create RateLimitingService with interval of {} seconds", pollingIntervalSeconds);
    return new RateLimitingService(Duration.ofSeconds(pollingIntervalSeconds));
  }
}
