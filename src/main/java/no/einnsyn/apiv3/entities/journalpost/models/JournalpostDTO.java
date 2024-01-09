// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.journalpost.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.features.validation.nossn.NoSSN;
import no.einnsyn.apiv3.features.validation.validationgroups.Insert;
import no.einnsyn.apiv3.features.validation.validationgroups.Update;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class JournalpostDTO extends RegistreringDTO {

  @Size(max = 500)
  private final String entity = "Journalpost";

  @NotNull(groups = { Insert.class })
  private Long journalaar;

  @NotNull(groups = { Insert.class })
  private Long journalsekvensnummer;

  @NotNull(groups = { Insert.class })
  private Long journalpostnummer;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String journalposttype;

  @Size(max = 500)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  @NotNull(groups = { Insert.class })
  private String journaldato;

  @Size(max = 500)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private String dokumentetsDato;

  @Size(max = 500)
  @NoSSN
  @Null(groups = { Insert.class, Update.class })
  private String sorteringstype;

  @Valid
  private ExpandableField<SaksmappeDTO> saksmappe;
}
