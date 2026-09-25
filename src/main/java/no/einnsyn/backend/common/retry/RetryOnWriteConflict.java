package no.einnsyn.backend.common.retry;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;

/**
 * Retries a transactional write that lost a race against a concurrent transaction. The retry runs
 * in a fresh transaction where the winner's row is visible, so it needs no delay. {@link
 * WriteConflictPredicate} decides what counts as a conflict.
 *
 * <p>Belongs on the method that opens the transaction. Nested uses are skipped rather than
 * forbidden, so the conflict reaches the outermost annotated method. Pure delegators should carry
 * neither this nor {@code @Transactional}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Retryable(
    includes = {
      ObjectOptimisticLockingFailureException.class,
      DataIntegrityViolationException.class
    },
    predicate = WriteConflictPredicate.class,
    delay = 0)
public @interface RetryOnWriteConflict {}
