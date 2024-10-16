package no.einnsyn.apiv3.entities.journalpost.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseES;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartES;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringES;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeES.SaksmappeWithoutChildrenES;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingES;

@Getter
@Setter
public class JournalpostES extends RegistreringES {
  private String journaldato;
  private String dokumentetsDato;
  private String journalpostnummer;
  private String journalposttype;
  private String journalaar;
  private String journalsekvensnummer;
  private List<String> saksnummerGenerert;
  private String standardDato;
  private SkjermingES skjerming;
  private SaksmappeWithoutChildrenES parent;
  private List<DokumentbeskrivelseES> dokumentbeskrivelse;
  private List<KorrespondansepartES> korrespondansepart;
}
