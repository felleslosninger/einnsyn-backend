package no.einnsyn.apiv3.entities.innsynskravdel.models;

import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.base.models.BaseES;

@Getter
@Setter
public class InnsynskravDelES extends BaseES {
  private String created;
  private String sent;
  private Boolean verified;
  private String bruker;
  private InnsynskravStat statRelation;

  @Getter
  @Setter
  public static class InnsynskravStat {
    private String name = "innsynskrav";
    private String parent;
  }
}
