package no.einnsyn.backend.configuration;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.authentication.AuthenticationService;
import no.einnsyn.backend.entities.enhet.EnhetService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Aspect that will enable / disable the "accessibleAfter" filters for entities based on the
 * authentication status of the user.
 */
@Aspect
@Component
@Slf4j
public class AccessibleFilterAspect {

  AuthenticationService authenticationService;
  EntityManager entityManager;
  EnhetService enhetService;

  public AccessibleFilterAspect(
      AuthenticationService authenticationService,
      EntityManager entityManager,
      EnhetService enhetService) {
    this.authenticationService = authenticationService;
    this.entityManager = entityManager;
    this.enhetService = enhetService;
  }

  @Before("@annotation(org.springframework.transaction.annotation.Transactional)")
  public void enableFilters(JoinPoint joinPoint) throws Throwable {
    var session = entityManager.unwrap(Session.class);

    // Remove filter for admins
    if (authenticationService.isAdmin()) {
      session.disableFilter("accessibleFilter");
      session.disableFilter("accessibleOrAdminFilter");
    } else {
      var journalenhetId = authenticationService.getJournalenhetId();
      var enhetList = enhetService.getSubtreeIds(journalenhetId);
      // Enable combined filter for requests authenticated as an Enhet
      if (!enhetList.isEmpty()) {
        log.debug("Enhet user detected, enabling combined filter for {}", journalenhetId);
        session.disableFilter("accessibleFilter");
        session.enableFilter("accessibleOrAdminFilter").setParameterList("journalenhet", enhetList);
      }
      // Enable standard accessibility filter for other authenticated requests
      else {
        log.debug("Authenticated user detected, enabling accessibility filter");
        session.enableFilter("accessibleFilter");
        session.disableFilter("accessibleOrAdminFilter");
      }
    }
  }
}
