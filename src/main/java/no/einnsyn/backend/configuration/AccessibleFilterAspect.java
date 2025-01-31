package no.einnsyn.backend.configuration;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.authentication.AuthenticationService;
import no.einnsyn.backend.entities.enhet.EnhetService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Aspect that will enable / disable the "accessibleAfter" filters for entities based on the
 * authentication status of the user.
 *
 * <p>This will be checked before each transactional method is executed.
 */
@Aspect
@Component
@Slf4j
// Execute after @Transactional, so filters are enabled *inside* the transaction:
@Order(Ordered.LOWEST_PRECEDENCE)
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

  /**
   * Enable / disable filters based on the authentication status of the user, at the start of each
   * transaction.
   */
  @Before("@annotation(org.springframework.transaction.annotation.Transactional)")
  public void enableFilters(JoinPoint joinPoint) throws Throwable {
    // Ensure logic runs only when a new transaction starts
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return;
    }

    var session = entityManager.unwrap(Session.class);

    // Remove filter for admins
    if (authenticationService.isAdmin()) {
      session.disableFilter("accessibleFilter");
      session.disableFilter("accessibleOrAdminFilter");
    } else {
      var journalenhetId = authenticationService.getEnhetId();
      var journalenhetSubtreeList = authenticationService.getEnhetSubtreeIdList();
      // Enable combined filter for requests authenticated as an Enhet
      if (!journalenhetSubtreeList.isEmpty()) {
        log.debug("Enhet user detected, enabling combined filter for {}", journalenhetId);
        session.disableFilter("accessibleFilter");
        session
            .enableFilter("accessibleOrAdminFilter")
            .setParameterList("journalenhet", journalenhetSubtreeList);
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
