package de.numcodex.feasibility_gui_backend.query.ratelimiting;

import de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.Jwt2AuthoritiesConverter;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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

  private final Jwt2AuthoritiesConverter jwt2AuthoritiesConverter;

  @Value("${app.keycloakAllowedRole}")
  private String keycloakAllowedRole;

  @Value("${app.keycloakAdminRole}")
  private String keycloakAdminRole;

  @Autowired
  public RateLimitingInterceptor(RateLimitingService rateLimitingService, Jwt2AuthoritiesConverter jwt2AuthoritiesConverter) {
    this.rateLimitingService = rateLimitingService;
    this.jwt2AuthoritiesConverter = jwt2AuthoritiesConverter;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    var principal = (Jwt)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    if (principal == null) {
      response.sendError(HttpStatus.UNAUTHORIZED.value());
      return false;
    }
    JwtAuthenticationToken jwtAuthenticationToken
        = new JwtAuthenticationToken(principal, jwt2AuthoritiesConverter.convert(principal));

    if (jwtAuthenticationToken.getAuthorities().stream().anyMatch(ga -> keycloakAdminRole.equals(ga.getAuthority()))) {
      return true;
    } else if (jwtAuthenticationToken.getAuthorities().stream().noneMatch(ga -> keycloakAllowedRole.equals(ga.getAuthority()))) {
      response.sendError(HttpStatus.FORBIDDEN.value());
      return false;
    }

    Bucket tokenBucket = rateLimitingService.resolveBucket(jwtAuthenticationToken.getName());
    ConsumptionProbe probe = tokenBucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      response.addHeader(HEADER_LIMIT_REMAINING, String.valueOf(probe.getRemainingTokens()));
      return true;
    } else {
      long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
      response.addHeader(HEADER_RETRY_AFTER, String.valueOf(waitForRefill));
      response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
          "You have exhausted your API Request Quota");
      return false;
    }
  }
}
