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
      @Value("${app.privacy.quota.read.any.pollingIntervalSeconds}") int pollingIntervalSeconds,
      @Value("${app.privacy.quota.read.detailedObfuscated.amount}") int detailedObfuscatedAmount,
      @Value("${app.privacy.quota.read.detailedObfuscated.intervalSeconds}") int detailedObfuscatedIntervalSeconds) {

    log.info(
        "Create RateLimitingService with interval of {} seconds for any result endpoint and {} allowed requests to detailed obfuscated result per {} seconds",
        pollingIntervalSeconds, detailedObfuscatedAmount, detailedObfuscatedIntervalSeconds);
    return new RateLimitingService(Duration.ofSeconds(pollingIntervalSeconds),
        detailedObfuscatedAmount, Duration.ofSeconds(detailedObfuscatedIntervalSeconds));
  }
}
