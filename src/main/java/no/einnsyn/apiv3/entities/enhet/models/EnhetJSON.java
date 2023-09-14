package no.einnsyn.apiv3.entities.enhet.models;

import java.time.LocalDate;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.features.validation.validationGroups.InsertValidationGroup;

@Getter
@Setter
public class EnhetJSON extends EinnsynObjectJSON {
  @NotNull(groups = {InsertValidationGroup.class})
  private String navn;

  private String navnNynorsk;

  private String navnEngelsk;

  private String navnSami;

  private LocalDate avsluttetDato;

  private ExpandableField<EnhetJSON> parent;

  private List<ExpandableField<EnhetJSON>> underEnheter;

  @Email
  private String innsynskravEpost;

  private String kontaktpunktAdresse;

  @Email
  private String kontaktpunktEpost;

  private String kontaktpunktTelefon;

  @Column(unique = true)
  private String orgnummer;

  @ManyToOne
  private ExpandableField<EnhetJSON> handteresAv;

  private String enhetsKode;

  @Enumerated(EnumType.STRING)
  @NotNull
  private Enhetstype type;

  private boolean skjult;

  private boolean eFormidling;

  private boolean visToppnode;

  private boolean erTeknisk;

  private boolean skalKonvertereId;

  private boolean skalMottaKvittering;

  private Integer orderXmlVersjon;
}
