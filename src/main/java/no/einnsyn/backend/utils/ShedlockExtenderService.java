package no.einnsyn.backend.utils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import net.javacrumbs.shedlock.core.LockExtender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShedlockExtenderService {

  /**
   * Extend the lock if more than half of the lock time has passed since last extension.
   *
   * @param lastExtended Timestamp in milliseconds when the lock was last extended.
   * @param lockExtendInterval Lock extension interval in milliseconds.
   * @return Updated timestamp in milliseconds when the lock was last extended.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public long maybeExtendLock(long lastExtended, int lockExtendInterval) {
    var now = System.currentTimeMillis();
    if (now - lastExtended > lockExtendInterval / 2) {
      LockExtender.extendActiveLock(
          Duration.of(lockExtendInterval, ChronoUnit.MILLIS),
          Duration.of(lockExtendInterval, ChronoUnit.MILLIS));
      return now;
    }
    return lastExtended;
  }
}
