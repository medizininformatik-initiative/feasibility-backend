package de.numcodex.feasibility_gui_backend.query.ratelimiting;

import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@Tag("query")
@Tag("ratelimiting")
@ExtendWith(MockitoExtension.class)
public class RateLimitingServiceTest {

  private final Duration interval = Duration.ofSeconds(1);
  private final int amountDetailedObfuscated = 2;
  private final Duration intervalDetailedObfuscated = Duration.ofSeconds(2);

  private RateLimitingService rateLimitingService;

  @BeforeEach
  void setUp() {
    this.rateLimitingService = new RateLimitingService(interval, amountDetailedObfuscated,
        intervalDetailedObfuscated);
  }

  @Test
  void testResolveBucket() {
    Bucket bucketSomeone = rateLimitingService.resolveAnyResultBucket("someone");
    assertNotNull(bucketSomeone);
    Bucket bucketSomeoneElse = rateLimitingService.resolveAnyResultBucket("someone-else");
    assertNotNull(bucketSomeoneElse);
    assertNotEquals(bucketSomeone, bucketSomeoneElse);
    assertEquals(bucketSomeone, rateLimitingService.resolveAnyResultBucket("someone"));
  }

  @Test
  void testResolveBucketRefill() throws InterruptedException {
    Bucket bucketSomeone = rateLimitingService.resolveAnyResultBucket("someone");
    assertTrue(bucketSomeone.tryConsume(1));
    assertFalse(bucketSomeone.tryConsume(1));
    Thread.sleep(TimeUnit.MILLISECONDS.convert(interval));
    assertTrue(bucketSomeone.tryConsume(1));
  }
}
