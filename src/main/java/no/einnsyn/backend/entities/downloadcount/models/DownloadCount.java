package no.einnsyn.backend.entities.downloadcount.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.indexable.Indexable;
import no.einnsyn.backend.entities.base.models.Base;
import no.einnsyn.backend.entities.enhet.models.Enhet;

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

  // The Journalpost, Moetesak or Moetemappe the downloads are attributed to in Elasticsearch. Kept
  // as a plain id since the parent may be deleted after the bucket is created.
  @Column(name = "parent__id")
  private String parentId;

  // The Enhet the parent was attributed to when the bucket was created
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enhet__id")
  private Enhet enhet;

  // lastIndexed should not be updated through JPA
  @Column(insertable = false, updatable = false)
  private Instant lastIndexed;
}
