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

  private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

  private final Duration interval;

  /**
   * Creates a new RateLimitingService.
   *
   * @param interval the duration after which the user can poll again
   */
  public RateLimitingService(Duration interval) {
    this.interval = interval;
  }

  public Bucket resolveBucket(String userId) {
    return cache.computeIfAbsent(userId, this::newBucket);
  }

  private Bucket newBucket(String userId) {
    return Bucket.builder()
        .addLimit(Bandwidth.classic(1, Refill.intervally(1, interval)))
        .build();
  }
}
