package no.einnsyn.backend.configuration;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.authentication.AuthenticationService;
import no.einnsyn.backend.entities.enhet.EnhetRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Aspect that will enable / disable the visibility filters (accessibleAfter, hidden Enhets) for
 * entities based on the authentication status of the user.
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
  EnhetRepository enhetRepository;

  public AccessibleFilterAspect(
      AuthenticationService authenticationService,
      EntityManager entityManager,
      EnhetRepository enhetRepository) {
    this.authenticationService = authenticationService;
    this.entityManager = entityManager;
    this.enhetRepository = enhetRepository;
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

    // Disable filter for non-web requests (scheduled tasks, etc.)
    if (RequestContextHolder.getRequestAttributes() == null) {
      log.trace("Non-web request detected, disabling accessibleFilter");
      session.disableFilter("accessibleFilter");
      session.disableFilter("accessibleOrAdminFilter");
    }

    // Disable filter for admins
    else if (authenticationService.isAdmin()) {
      log.trace("Admin user detected, disabling accessibleFilter");
      session.disableFilter("accessibleFilter");
      session.disableFilter("accessibleOrAdminFilter");
    }

    // All other web requests
    else {
      var journalenhetId = authenticationService.getEnhetId();
      var journalenhetSubtreeList = authenticationService.getEnhetSubtreeIdList();

      // Enable combined filter for requests authenticated as an Enhet
      if (!journalenhetSubtreeList.isEmpty()) {
        var currentFilter = session.getEnabledFilter("accessibleOrAdminFilter");

        // If the filter is already enabled with the same parameters, do nothing
        if (currentFilter != null
            && Objects.equals(
                currentFilter.getParameterValue("journalenhet"), journalenhetSubtreeList)) {
          return;
        }

        log.trace(
            "Enhet user detected, enabling combined accessibleOrAdminFilter for {}",
            journalenhetId);
        session.disableFilter("accessibleFilter");
        session
            .enableFilter("accessibleOrAdminFilter")
            .setParameterList("journalenhet", journalenhetSubtreeList)
            .setParameterList("hiddenEnhet", getHiddenEnhetIdList());
      }

      // Enable standard accessibility filter for other authenticated requests
      else {
        if (session.getEnabledFilter("accessibleFilter") != null) {
          return;
        }

        log.trace("Authenticated user detected, enabling accessibleFilter");
        session
            .enableFilter("accessibleFilter")
            .setParameterList("hiddenEnhet", getHiddenEnhetIdList());
        session.disableFilter("accessibleOrAdminFilter");
      }
    }
  }

  /** Enhets in a hidden subtree, whose objects are not public. */
  private List<String> getHiddenEnhetIdList() {
    var hiddenEnhetIdList = enhetRepository.getHiddenSubtreeIdList();
    // Hibernate renders an empty parameter list as "()", which is invalid SQL
    return hiddenEnhetIdList.isEmpty() ? List.of("") : hiddenEnhetIdList;
  }
}
