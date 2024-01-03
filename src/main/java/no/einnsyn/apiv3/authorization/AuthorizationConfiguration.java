package no.einnsyn.apiv3.authorization;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class AuthorizationConfiguration {

  // TODO: Deny access to all endpoints that are not explicitly allowed using @PreAuthorize
}
