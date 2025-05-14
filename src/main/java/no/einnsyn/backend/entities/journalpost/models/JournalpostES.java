package no.einnsyn.backend.entities.journalpost.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseES;
import no.einnsyn.backend.entities.korrespondansepart.models.KorrespondansepartES;
import no.einnsyn.backend.entities.registrering.models.RegistreringES;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeES.SaksmappeWithoutChildrenES;
import no.einnsyn.backend.entities.skjerming.models.SkjermingES;

@Getter
@Setter
public class JournalpostES extends RegistreringES {
  private String journaldato;
  private String dokumentetsDato;
  private String journalpostnummer;
  private String journalposttype;
  private String journalaar;
  private String journalsekvensnummer;
  private String saksaar;
  private String sakssekvensnummer;
  private String saksnummer;
  private List<String> saksnummerGenerert;
  private String standardDato;
  private SkjermingES skjerming;
  private SaksmappeWithoutChildrenES parent;
  private List<DokumentbeskrivelseES> dokumentbeskrivelse;
  private List<KorrespondansepartES> korrespondansepart;
}
