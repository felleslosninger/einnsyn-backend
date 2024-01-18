package no.einnsyn.apiv3.common.indexable;

import java.time.Instant;

public interface Indexable {

  Instant getLastIndexed();

  void setLastIndexed(Instant lastIndexed);
}
