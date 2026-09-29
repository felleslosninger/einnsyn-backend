package no.einnsyn.backend.common.retry;

import java.lang.reflect.Method;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.resilience.retry.MethodRetryPredicate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Retry policy for {@link RetryOnWriteConflict}: optimistic lock failures, and unique violations
 * from a row our pre-checks did not see. Not-null, foreign key and check violations are
 * deterministic and would fail the same way on every attempt.
 */
public class WriteConflictPredicate implements MethodRetryPredicate {

  /** PostgreSQL SQLSTATE for unique_violation. */
  static final String UNIQUE_VIOLATION_SQLSTATE = "23505";

  /**
   * {@inheritDoc}
   *
   * <p>Never retries while a transaction is active. Retry advice runs outside transaction advice,
   * so an active transaction here means we joined a caller's, which the failure has already
   * aborted; retrying would only mask the conflict from the outermost annotated method. This also
   * skips an annotated {@code REQUIRES_NEW} method, which could safely retry. None exists today.
   */
  @Override
  public boolean shouldRetry(Method method, Throwable throwable) {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      return false;
    }
    return isWriteConflict(throwable);
  }

  private boolean isWriteConflict(Throwable throwable) {
    for (var cause = throwable; cause != null; cause = cause.getCause()) {
      if (cause instanceof ObjectOptimisticLockingFailureException) {
        return true;
      }
      // Hibernate classifies the violation for us. Fall through to the SQLException if it couldn't.
      if (cause instanceof ConstraintViolationException cve
          && cve.getKind() == ConstraintKind.UNIQUE) {
        return true;
      }
      if (cause instanceof SQLException sqlException) {
        return UNIQUE_VIOLATION_SQLSTATE.equals(sqlException.getSQLState());
      }
    }
    return false;
  }
}
