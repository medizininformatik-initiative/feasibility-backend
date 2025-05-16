package de.numcodex.feasibility_gui_backend.query.ratelimiting;

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
      @Value("${app.privacy.quota.read.resultSummary.pollingInterval}") String pollingIntervalSummary,
      @Value("${app.privacy.quota.read.resultDetailedObfuscated.pollingInterval}") String pollingIntervalDetailed,
      @Value("${app.privacy.quota.read.resultDetailedObfuscated.amount}") int detailedObfuscatedAmount,
      @Value("${app.privacy.quota.read.resultDetailedObfuscated.interval}") String detailedObfuscatedInterval) {

    log.info(
        "Create RateLimitingService with interval of {} for summary result endpoint, {} for detailed"
            + " obfuscated results and {} allowed requests to detailed obfuscated result per {}",
        pollingIntervalSummary, pollingIntervalDetailed, detailedObfuscatedAmount, detailedObfuscatedInterval);
    return new RateLimitingService(Duration.parse(pollingIntervalSummary),
        Duration.parse(pollingIntervalDetailed),
        detailedObfuscatedAmount, Duration.parse(detailedObfuscatedInterval));
  }
}
