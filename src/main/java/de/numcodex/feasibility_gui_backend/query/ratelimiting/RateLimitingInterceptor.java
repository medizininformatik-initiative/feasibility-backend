package de.numcodex.feasibility_gui_backend.query.ratelimiting;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * This Interceptor checks whether a user may use the requested endpoint at this moment.
 * If the user has the admin role (as defined via config), he is not subject to rate-limiting.
 */
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

  private static final String HEADER_LIMIT_REMAINING = "X-Rate-Limit-Remaining";
  private static final String HEADER_RETRY_AFTER = "X-Rate-Limit-Retry-After-Seconds";
  private final RateLimitingService rateLimitingService;

  private final AuthenticationHelper authenticationHelper;

  @Value("${app.keycloakAllowedRole}")
  private String keycloakAllowedRole;

  @Value("${app.keycloakAdminRole}")
  private String keycloakAdminRole;

  @Autowired
  public RateLimitingInterceptor(RateLimitingService rateLimitingService, AuthenticationHelper authenticationHelper) {
    this.rateLimitingService = rateLimitingService;
    this.authenticationHelper = authenticationHelper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
      response.sendError(HttpStatus.UNAUTHORIZED.value());
      return false;
    }

    if (authenticationHelper.hasAuthority(authentication, keycloakAdminRole)) {
      return true;
    } else if (!authenticationHelper.hasAuthority(authentication, keycloakAllowedRole)) {
      response.sendError(HttpStatus.FORBIDDEN.value());
      return false;
    }

    Bucket tokenBucket = rateLimitingService.resolveBucket(authentication.getName());
    ConsumptionProbe probe = tokenBucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      response.addHeader(HEADER_LIMIT_REMAINING, Long.toString(probe.getRemainingTokens()));
      return true;
    } else {
      long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
      response.addHeader(HEADER_RETRY_AFTER, Long.toString(waitForRefill));
      response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
          "You have exhausted your API Request Quota");
      return false;
    }
  }
}
