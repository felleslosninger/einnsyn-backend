package no.einnsyn.backend.common.retry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLState;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class WriteConflictPredicateTest {

  private final WriteConflictPredicate predicate = new WriteConflictPredicate();
  private final Method method = getClass().getDeclaredMethods()[0];

  /** Builds the chain Spring Data JPA produces when a flush hits a constraint. */
  private static DataIntegrityViolationException constraintViolation(
      ConstraintKind kind, PSQLState sqlState) {
    var sqlException = new SQLException("ERROR: violates constraint", sqlState.getState());
    var hibernateException =
        new ConstraintViolationException("could not execute statement", sqlException, kind, "c");
    return new DataIntegrityViolationException(hibernateException.getMessage(), hibernateException);
  }

  @Test
  void retriesUniqueConstraintViolation() {
    assertTrue(
        predicate.shouldRetry(
            method, constraintViolation(ConstraintKind.UNIQUE, PSQLState.UNIQUE_VIOLATION)));
  }

  @Test
  void doesNotRetryOtherConstraintViolations() {
    assertFalse(
        predicate.shouldRetry(
            method,
            constraintViolation(ConstraintKind.FOREIGN_KEY, PSQLState.FOREIGN_KEY_VIOLATION)));
    assertFalse(
        predicate.shouldRetry(
            method, constraintViolation(ConstraintKind.NOT_NULL, PSQLState.NOT_NULL_VIOLATION)));
    assertFalse(
        predicate.shouldRetry(
            method, constraintViolation(ConstraintKind.CHECK, PSQLState.CHECK_VIOLATION)));
  }

  @Test
  void fallsBackToSqlStateWhenHibernateCouldNotClassify() {
    assertTrue(
        predicate.shouldRetry(
            method, constraintViolation(ConstraintKind.OTHER, PSQLState.UNIQUE_VIOLATION)));
    assertFalse(
        predicate.shouldRetry(
            method, constraintViolation(ConstraintKind.OTHER, PSQLState.FOREIGN_KEY_VIOLATION)));
  }

  @Test
  void retriesUniqueViolationWithoutHibernateInTheChain() {
    var exception =
        new DataIntegrityViolationException(
            "duplicate key", new SQLException("dup", PSQLState.UNIQUE_VIOLATION.getState()));
    assertTrue(predicate.shouldRetry(method, exception));
  }

  @Test
  void doesNotRetryIntegrityViolationWithoutCause() {
    assertFalse(predicate.shouldRetry(method, new DataIntegrityViolationException("no cause")));
  }

  @Test
  void alwaysRetriesOptimisticLockFailure() {
    var exception = new ObjectOptimisticLockingFailureException(Object.class, "id");
    assertTrue(predicate.shouldRetry(method, exception));
    assertTrue(predicate.shouldRetry(method, new RuntimeException("wrapped", exception)));
  }

  @Test
  void doesNotRetryUnrelatedExceptions() {
    assertFalse(predicate.shouldRetry(method, new IllegalStateException("boom")));
  }

  /** An active transaction means we joined a caller's, which the failure has already aborted. */
  @Test
  void doesNotRetryWhileAnOuterTransactionIsActive() {
    TransactionSynchronizationManager.setActualTransactionActive(true);

    assertFalse(
        predicate.shouldRetry(
            method, constraintViolation(ConstraintKind.UNIQUE, PSQLState.UNIQUE_VIOLATION)));
    assertFalse(
        predicate.shouldRetry(
            method, new ObjectOptimisticLockingFailureException(Object.class, "id")));
  }

  @AfterEach
  void clearTransactionState() {
    TransactionSynchronizationManager.setActualTransactionActive(false);
  }
}
