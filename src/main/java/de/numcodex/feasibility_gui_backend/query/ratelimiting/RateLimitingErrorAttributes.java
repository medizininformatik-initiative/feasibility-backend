package de.numcodex.feasibility_gui_backend.query.ratelimiting;

import de.numcodex.feasibility_gui_backend.query.api.status.FeasibilityIssue;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Map;

@Component
public class RateLimitingErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        String originalErrorMessage = super.getMessage(webRequest, null);

        if (FeasibilityIssue.QUOTA_EXCEEDED.name().equals(originalErrorMessage)) {
            return Map.of("issue", List.of(FeasibilityIssue.QUOTA_EXCEEDED));
        } else if (FeasibilityIssue.POLLING_LIMIT_EXCEEDED.name().equals(originalErrorMessage))
            return Map.of("issue", List.of(FeasibilityIssue.POLLING_LIMIT_EXCEEDED));

        return super.getErrorAttributes(webRequest, options);
    }
}
