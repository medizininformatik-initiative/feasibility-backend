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

  private final Map<String, Bucket> anyResultRetrievalCache = new ConcurrentHashMap<>();
  private final Map<String, Bucket> detailedObfuscatedResultRetrievalCache = new ConcurrentHashMap<>();

  private final Duration intervalAny;

  private final Duration intervalDetailedObfuscated;

  private final int amountDetailedObfuscated;

  /**
   * Creates a new RateLimitingService.
   * <p>
   * This will limit the polling rate for default users on any result endpoint. It will also limit
   * the amount of times a user can request detailed obfuscated results in a given timespan.
   *
   * @param intervalAny the duration after which the user can poll again
   * @param amountDetailedObfuscated the amount of times a user can request detailed obfuscated results
   * @param intervalDetailedObfuscated the timespan after which a users access is "forgotten"
   */
  public RateLimitingService(Duration intervalAny, int amountDetailedObfuscated, Duration intervalDetailedObfuscated) {
    this.intervalAny = intervalAny;
    this.amountDetailedObfuscated = amountDetailedObfuscated;
    this.intervalDetailedObfuscated = intervalDetailedObfuscated;
  }

  public Bucket resolveAnyResultBucket(String userId) {
    return anyResultRetrievalCache.computeIfAbsent(userId, this::newAnyResultBucket);
  }

  public Bucket resolveDetailedObfuscatedResultBucket(String userId) {
    return detailedObfuscatedResultRetrievalCache.computeIfAbsent(userId, this::newDetailedObfuscatedResultBucket);
  }

  public void addTokensToDetailedObfuscatedResultBucket(String userId, int amount) {
    resolveDetailedObfuscatedResultBucket(userId).addTokens(amount);
  }

  private Bucket newAnyResultBucket(String userId) {
    return Bucket.builder()
        .addLimit(Bandwidth.classic(1, Refill.intervally(1, intervalAny)))
        .build();
  }

  private Bucket newDetailedObfuscatedResultBucket(String userId) {
    return Bucket.builder()
        .addLimit(Bandwidth.classic(amountDetailedObfuscated, Refill.intervally(1, intervalDetailedObfuscated)))
        .build();
  }


}
