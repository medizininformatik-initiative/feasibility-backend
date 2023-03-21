package de.numcodex.feasibility_gui_backend.query.ratelimiting;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implement a rate-limiting service via a {@link Bucket} on a per-user basis.
 * <p>
 * The limit is defined via application.yaml or env variable.
 */
public class RateLimitingService {

  private final Map<String, Bucket> summaryResultRetrievalCache = new ConcurrentHashMap<>();
  private final Map<String, Bucket> detailedResultRetrievalCache = new ConcurrentHashMap<>();
  private final Map<String, Bucket> detailedViewObfuscatedResultRetrievalCache = new ConcurrentHashMap<>();

  private final Duration intervalPollingSummary;
  private final Duration intervalPollingDetailed;
  private final Duration intervalDetailedObfuscated;
  private final int amountDetailedObfuscated;

  /**
   * Creates a new RateLimitingService.
   * <p>
   * This will limit the polling rate for default users on any result endpoint. It will also limit
   * the amount of times a user can request detailed obfuscated results in a given timespan.
   *
   * @param intervalPollingSummary the duration after which the user can poll summary results again
   * @param intervalPollingDetailed the duration after which the user can poll detailed results again
   * @param amountDetailedObfuscated the amount of times a user can request detailed obfuscated results
   * @param intervalDetailedObfuscated the timespan after which a users access is "forgotten"
   */
  public RateLimitingService(Duration intervalPollingSummary, Duration intervalPollingDetailed, int amountDetailedObfuscated, Duration intervalDetailedObfuscated) {
    this.intervalPollingSummary = intervalPollingSummary;
    this.intervalPollingDetailed = intervalPollingDetailed;
    this.amountDetailedObfuscated = amountDetailedObfuscated;
    this.intervalDetailedObfuscated = intervalDetailedObfuscated;
  }

  public Bucket resolveSummaryResultBucket(String userId) {
    return summaryResultRetrievalCache.computeIfAbsent(userId, this::newSummaryResultBucket);
  }

  public Bucket resolveDetailedObfuscatedResultBucket(String userId) {
    return detailedResultRetrievalCache.computeIfAbsent(userId, this::newDetailedResultBucket);
  }

  public Bucket resolveViewDetailedObfuscatedBucket(String userId) {
    return detailedViewObfuscatedResultRetrievalCache.computeIfAbsent(userId, this::newViewDetailedObfuscatedResultBucket);
  }

  public void addTokensToDetailedObfuscatedResultBucket(String userId, int amount) {
    resolveViewDetailedObfuscatedBucket(userId).addTokens(amount);
  }

  private Bucket newSummaryResultBucket(String userId) {
    return Bucket.builder()
        .addLimit(Bandwidth.classic(1, Refill.intervally(1, intervalPollingSummary)))
        .build();
  }

  private Bucket newDetailedResultBucket(String userId) {
    return Bucket.builder()
        .addLimit(Bandwidth.classic(1, Refill.intervally(1, intervalPollingDetailed)))
        .build();
  }

  public int getAmountDetailedObfuscated(){
    return this.amountDetailedObfuscated;
  }

  private Bucket newViewDetailedObfuscatedResultBucket(String userId) {
    return Bucket.builder()
        .addLimit(Bandwidth.classic(amountDetailedObfuscated, Refill.intervally(1, intervalDetailedObfuscated)))
        .build();
  }


}
