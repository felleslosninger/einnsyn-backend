package no.einnsyn.apiv3.entities.innsynskrav;

import java.util.ArrayList;
import java.util.List;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("java:S1192") // Allow multiple constants
public class OrderFileGenerator {

  private final JournalpostService journalpostService;

  public OrderFileGenerator(JournalpostService journalpostService) {
    this.journalpostService = journalpostService;
  }

  String toOrderXML(Enhet enhet, Innsynskrav innsynskrav, List<InnsynskravDel> innsynskravDelList) {
    var orderXmlVersion = enhet.getOrderXmlVersjon();
    if (orderXmlVersion == null) {
      orderXmlVersion = 1;
    }
    if (orderXmlVersion == 2) {
      return toOrderXMLV2(enhet, innsynskrav, innsynskravDelList);
    }
    return toOrderXMLV1(enhet, innsynskrav, innsynskravDelList);
  }

  String toOrderXMLV1(
      Enhet enhet, Innsynskrav innsynskrav, List<InnsynskravDel> innsynskravDelList) {
    return XML.toString(toOrderJSONV1(enhet, innsynskrav, innsynskravDelList), 2);
  }

  String toOrderXMLV2(
      Enhet enhet, Innsynskrav innsynskrav, List<InnsynskravDel> innsynskravDelList) {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + XML.toString(toOrderJSONV2(enhet, innsynskrav, innsynskravDelList), 2);
  }

  /**
   * Generate a order JSON for each Enhet
   *
   * @param innsynskrav The innsynskrav
   * @return The order JSON
   */
  JSONObject toOrderJSONV1(
      Enhet enhet, Innsynskrav innsynskrav, List<InnsynskravDel> innsynskravDelList) {
    // bestilling
    // - id
    // - bestillingsdato
    // - til
    // - - enhet
    // - - enhetepost
    // - innsynskravepost
    // - kontaktinfo
    // - - forsendelsesmåte: e-post
    // - - navn
    // - - organisasjon
    // - - land
    // - - e-post
    // - dokumenter
    // - - dokument
    // - - - saksnr
    // - - - dokumentnr
    // - - - journalnr
    // - - - saksbehandler

    var bestilling =
        new JSONObject()
            .put("id", innsynskrav.getId())
            .put("bestillingsdato", innsynskrav.getOpprettetDato())
            .put(
                "til",
                new JSONObject()
                    .put("enhet", enhet.getNavn())
                    .put("enhetepost", enhet.getKontaktpunktEpost()))
            .put("innsynskravepost", enhet.getInnsynskravEpost())
            .put(
                "kontaktinfo",
                new JSONObject()
                    .put("forsendelsesmåte", "e-post")
                    .put("e-post", innsynskrav.getEpost())
                // .put("navn", innsynskrav.getNavn())
                // .put("organisasjon", innsynskrav.getOrganisasjon())
                // .put("land", innsynskrav.getLand())
                )
            .put("dokumenter", new ArrayList<>());

    for (var innsynskravDel : innsynskravDelList) {
      // Generate saksnummer
      var journalpost = innsynskravDel.getJournalpost();
      var journalpostId = journalpost.getId();
      var saksmappe = journalpost.getSaksmappe();
      var saksaar = saksmappe.getSaksaar();
      var saksnummer =
          (saksaar > 100 ? saksaar : saksaar + 1900) + "/" + saksmappe.getSakssekvensnummer();

      // Add this document to "dokumenter" list
      var dokumenter = bestilling.getJSONArray("dokumenter");
      dokumenter.put(
          new JSONObject()
              .put(
                  "dokument",
                  new JSONObject()
                      .put("saksnr", saksnummer)
                      .put("dokumentnr", journalpost.getJournalpostnummer())
                      .put("journalnr", journalpost.getJournalsekvensnummer())
                      .put("saksbehandler", journalpostService.getSaksbehandler(journalpostId))));
    }

    return new JSONObject().put("bestilling", bestilling);
  }

  // TODO: Add doctype header
  public JSONObject toOrderJSONV2(
      Enhet enhet, Innsynskrav innsynskrav, List<InnsynskravDel> innsynskravDelList) {
    // ns2:bestilling
    // - id
    // - bestillingsdato
    // - til
    // - - virksomhet
    // - - orgnr
    // - - enhetepost
    // - - innsynskravepost
    // - kontaktinfo
    // - - forsendelsesmåte: e-post
    // - - e-post
    // - dokumenter
    // - - dokument
    // - - - id
    // - - - systemId
    // - - - saksnr
    // - - - dokumentnr
    // - - - journalnr
    // - - - saksbehandler
    // - - - admEnhet
    // - - - fagsysteminfo
    // - - - - id
    // - - - - delId

    var bestilling =
        new JSONObject()
            .put("id", innsynskrav.getId())
            .put("bestillingsdato", innsynskrav.getOpprettetDato())
            .put(
                "til",
                new JSONObject()
                    .put("virksomhet", enhet.getNavn())
                    .put("orgnr", enhet.getOrgnummer())
                    .put("enhetepost", enhet.getKontaktpunktEpost())
                    .put("innsynskravepost", enhet.getInnsynskravEpost()))
            .put(
                "kontaktinfo",
                new JSONObject()
                    .put("forsendelsesmåte", "e-post")
                    .put("e-post", innsynskrav.getEpost()))
            .put("dokumenter", new ArrayList<>());

    for (var innsynskravDel : innsynskravDelList) {
      // Generate saksnummer
      var journalpost = innsynskravDel.getJournalpost();
      var journalpostId = journalpost.getId();
      var saksmappe = journalpost.getSaksmappe();
      var saksaar = saksmappe.getSaksaar();
      var saksnummer =
          (saksaar > 100 ? saksaar : saksaar + 1900) + "/" + saksmappe.getSakssekvensnummer();

      // Add this document to "dokumenter" list
      var dokumenter = bestilling.getJSONArray("dokumenter");
      dokumenter.put(
          new JSONObject()
              .put(
                  "dokument",
                  new JSONObject()
                      .put("fagsysteminfo", "") // TODO
                      .put("id", journalpost.getId())
                      .put("systemId", "") // TODO
                      .put("saksnr", saksnummer)
                      .put("dokumentnr", journalpost.getJournalpostnummer())
                      .put("journalnr", journalpost.getJournalsekvensnummer())
                      .put("saksbehandler", journalpostService.getSaksbehandler(journalpostId))
                      .put(
                          "admEnhet",
                          journalpostService.getAdministrativEnhetKode(journalpostId))));
    }

    return new JSONObject().put("ns2:bestilling", bestilling);
  }
}
