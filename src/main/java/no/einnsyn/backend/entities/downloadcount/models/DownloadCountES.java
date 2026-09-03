package no.einnsyn.backend.entities.downloadcount.models;

import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.BaseES;

@Getter
@Setter
public class DownloadCountES extends BaseES {
  private Integer count;
  private DownloadCountRelation statRelation;

  @Getter
  @Setter
  public static class DownloadCountRelation {
    private String name = "download";
    private String parent;
  }
}
