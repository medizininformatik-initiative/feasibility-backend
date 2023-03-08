package de.numcodex.feasibility_gui_backend.query.ratelimiting;

import de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.Jwt2AuthoritiesConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class PrincipalHelper {

  private final Jwt2AuthoritiesConverter jwt2AuthoritiesConverter;

  @Autowired
  public PrincipalHelper(Jwt2AuthoritiesConverter jwt2AuthoritiesConverter) {
    this.jwt2AuthoritiesConverter = jwt2AuthoritiesConverter;
  }

  public String getName(Object principal) throws InvalidPrincipalException {
    if (principal.getClass() == Jwt.class) {
      return ((Jwt) principal).getSubject();
    } else {
      throw new InvalidPrincipalException();
    }
  }

  public boolean hasAuthority(Object principal, String authority) throws InvalidPrincipalException {
    if (principal.getClass() == Jwt.class) {
      var jwtPrincipal = (Jwt) principal;
      JwtAuthenticationToken jwtAuthenticationToken
          = new JwtAuthenticationToken(jwtPrincipal,
          jwt2AuthoritiesConverter.convert(jwtPrincipal));

      return jwtAuthenticationToken.getAuthorities().stream()
          .anyMatch(auth -> authority.equals(auth.getAuthority()));
    } else {
      throw new InvalidPrincipalException();
    }
  }

}
