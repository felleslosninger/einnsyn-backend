package no.einnsyn.apiv3.entities.innsynskravdel.models;

import java.util.Date;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Table(indexes = @Index(columnList = "innsynskravDel, status"))
public class InnsynskravDelStatus {

  @NotNull
  private Date opprettetDato;

  @NotNull
  @Enumerated(EnumType.STRING)
  private InnsynskravDelStatusValue status;

  @NotNull
  private boolean systemgenerert;

  @ManyToOne
  @NotNull
  @JoinColumn(name = "innsynskrav_del_id")
  private InnsynskravDel innsynskravDel;
}
