package no.einnsyn.backend.entities.downloadcount.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.indexable.Indexable;
import no.einnsyn.backend.entities.base.models.Base;

/**
 * Hourly download bucket for a Dokumentobjekt.
 *
 * <p>A bucket is a record of downloads that happened, not a part of the file. It is never deleted
 * with the Dokumentobjekt, so the attribution needed by statistics is captured when the bucket is
 * created, while the file and its parents still exist.
 */
@Getter
@Setter
@Table(name = "dokumentobjekt_download_stat")
@Entity
public class DownloadCount extends Base implements Indexable {

  @NotNull
  @Column(name = "dokumentobjekt__id")
  private String dokumentobjektId;

  @NotNull private Instant bucketStart;

  @NotNull
  @Column(name = "download_count")
  private int count;

  // The Journalpost, Moetesak or Moetemappe the downloads are attributed to in Elasticsearch, and
  // the Enhet that parent was attributed to. Both are kept as plain ids rather than relations: the
  // parent or the Enhet may be deleted after the bucket is created, and the bucket must keep
  // recording what the attribution was at the time.
  @Column(name = "parent__id")
  private String parentId;

  @Column(name = "enhet__id")
  private String enhetId;

  // lastIndexed should not be updated through JPA
  @Column(insertable = false, updatable = false)
  private Instant lastIndexed;
}
