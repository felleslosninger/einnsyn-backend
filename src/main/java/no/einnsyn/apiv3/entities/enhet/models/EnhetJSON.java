package no.einnsyn.apiv3.entities.enhet.models;

import java.time.LocalDate;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.features.validation.validationgroups.Insert;

@Getter
@Setter
public class EnhetJSON extends EinnsynObjectJSON {
  @NotNull(groups = {Insert.class})
  private String navn;

  private String navnNynorsk;

  private String navnEngelsk;

  private String navnSami;

  private LocalDate avsluttetDato;

  @ExistingObject(type = Enhet.class)
  private ExpandableField<EnhetJSON> parent;

  private List<ExpandableField<EnhetJSON>> underenhet;

  @Email
  private String innsynskravEpost;

  private String kontaktpunktAdresse;

  @Email
  private String kontaktpunktEpost;

  private String kontaktpunktTelefon;

  @Column(unique = true)
  private String orgnummer;

  private ExpandableField<EnhetJSON> handteresAv;

  private String enhetskode;

  @Enumerated(EnumType.STRING)
  @NotNull(groups = {Insert.class})
  private Enhetstype enhetstype;

  private Boolean skjult;

  private Boolean eFormidling;

  private Boolean visToppnode;

  private Boolean erTeknisk;

  private Boolean skalKonvertereId;

  private Boolean skalMottaKvittering;

  private Integer orderXmlVersjon;
}
