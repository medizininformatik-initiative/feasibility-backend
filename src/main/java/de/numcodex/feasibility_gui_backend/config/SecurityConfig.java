package de.numcodex.feasibility_gui_backend.config;

import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

@KeycloakConfiguration
@ConditionalOnProperty(name = "security.config.use-keycloak", havingValue = "true", matchIfMissing = true)
class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

  @Value("${app.keycloakAllowedRole}")
  private String keycloakAllowedRole;

  @Value("${app.keycloakAdminRole}")
  private String keycloakAdminRole;

  public static void configureApiSecurity(HttpSecurity http) throws Exception {
    http
        .authorizeRequests()
        .anyRequest().authenticated()
        .and()
        .csrf().disable();
  }

  @Autowired
  public void configureGlobal(
      AuthenticationManagerBuilder auth) throws Exception {

    KeycloakAuthenticationProvider keycloakAuthenticationProvider
        = keycloakAuthenticationProvider();
    keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(
        new SimpleAuthorityMapper());
    auth.authenticationProvider(keycloakAuthenticationProvider);
  }

  @Bean
  @Override
  protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
    return new RegisterSessionAuthenticationStrategy(
        new SessionRegistryImpl());
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    super.configure(http);
    // TODO: discuss if this is needed or can be left at the default ("IF_REQUIRED")
    http.sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    http.authorizeRequests()
        .antMatchers("/api/v1/**").hasRole(keycloakAllowedRole)
        .antMatchers("/api/v2/query").hasRole(keycloakAllowedRole)
        .antMatchers("/api/v2/query/{id:\\d+}/saved").hasRole(keycloakAllowedRole)
        .antMatchers("/api/v2/query/by-user/{id:\\w+}").hasRole(keycloakAdminRole)
        .antMatchers("/api/v2/query/{id:\\d+}").hasAnyRole(keycloakAdminRole, keycloakAllowedRole)
        .antMatchers("/api/v2/query/{id:\\d+}/result/detailed").hasRole(keycloakAdminRole)
        .antMatchers("/api/v2/query/{id:\\d+}/result").hasAnyRole(keycloakAdminRole, keycloakAllowedRole)
        .antMatchers("/api/v2/query/{id:\\d+}/content").hasAnyRole(keycloakAdminRole, keycloakAllowedRole)
        .anyRequest()
        .permitAll()
        .and()
        .csrf().disable();
  }
}

