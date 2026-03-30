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

  // lastIndexed should not be updated through JPA
  @Column(insertable = false, updatable = false)
  private Instant lastIndexed;
}
