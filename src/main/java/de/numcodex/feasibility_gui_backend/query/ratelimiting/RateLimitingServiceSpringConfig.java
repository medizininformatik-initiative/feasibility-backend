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
      @Value("${app.privacy.quota.read.resultSummary.pollingIntervalSeconds}") int pollingIntervalSecondsSummary,
      @Value("${app.privacy.quota.read.resultDetailedObfuscated.pollingIntervalSeconds}") int pollingIntervalSecondsDetailed,
      @Value("${app.privacy.quota.read.resultDetailedObfuscated.amount}") int detailedObfuscatedAmount,
      @Value("${app.privacy.quota.read.resultDetailedObfuscated.intervalSeconds}") int detailedObfuscatedIntervalSeconds) {

    log.info(
        "Create RateLimitingService with interval of {} seconds for summary result endpoint, {} seconds for detailed"
            + "obfuscated results and {} allowed requests to detailed obfuscated result per {} seconds",
        pollingIntervalSecondsSummary, pollingIntervalSecondsDetailed, detailedObfuscatedAmount, detailedObfuscatedIntervalSeconds);
    return new RateLimitingService(Duration.ofSeconds(pollingIntervalSecondsSummary),
        Duration.ofSeconds(pollingIntervalSecondsDetailed),
        detailedObfuscatedAmount, Duration.ofSeconds(detailedObfuscatedIntervalSeconds));
  }
}
