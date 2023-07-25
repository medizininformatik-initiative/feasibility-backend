package de.numcodex.feasibility_gui_backend.config;

import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.PATH_API;
import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT;
import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.PATH_QUERY;
import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.PATH_ID_MATCHER;
import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.PATH_SUMMARY_RESULT;

import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class RateLimitingConfig implements WebMvcConfigurer {

  @Autowired
  @Lazy
  private RateLimitingInterceptor interceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(interceptor)
        .addPathPatterns(PATH_API + PATH_QUERY + PATH_ID_MATCHER + PATH_SUMMARY_RESULT)
        .addPathPatterns(PATH_API + PATH_QUERY + PATH_ID_MATCHER + PATH_DETAILED_OBFUSCATED_RESULT);
  }
}
