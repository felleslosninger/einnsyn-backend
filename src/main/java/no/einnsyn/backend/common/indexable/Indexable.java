package no.einnsyn.backend.common.indexable;

import java.time.Instant;

public interface Indexable {

  Instant getLastIndexed();

  void setLastIndexed(Instant lastIndexed);
}
