// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.registrering.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.korrespondansepart.KorrespondansepartService;
import no.einnsyn.backend.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

/** Registrering */
@Getter
@Setter
public class RegistreringDTO extends ArkivBaseDTO {
  /** The title of the resource, with sensitive information redacted. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String offentligTittel;

  /** The title of the resource, with sensitive information included. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String offentligTittelSensitiv;

  @NoSSN
  @Size(max = 500)
  protected String beskrivelse;

  /**
   * The date the resource was published. This field is updated automatically, but can be set
   * manually by admins.
   */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  protected String publisertDato;

  /**
   * The date the resource was last updated. This field is updated automatically, but can be set
   * manually by admins.
   */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  protected String oppdatertDato;

  @ExpandableObject(
      service = KorrespondansepartService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected List<ExpandableField<KorrespondansepartDTO>> korrespondansepart;

  @ExpandableObject(
      service = DokumentbeskrivelseService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected List<ExpandableField<DokumentbeskrivelseDTO>> dokumentbeskrivelse;

  /** The administrative unit that has been handed the responsibility for this resource. */
  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<EnhetDTO> avhendetTil;
}
