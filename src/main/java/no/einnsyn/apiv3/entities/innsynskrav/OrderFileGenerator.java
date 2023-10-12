package no.einnsyn.apiv3.entities.innsynskrav;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.json.XML;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;

public class OrderFileGenerator {

  static String toOrderXML(Enhet enhet, Innsynskrav innsynskrav,
      List<InnsynskravDel> innsynskravDelList) {
    if (enhet.getOrderXmlVersjon() == 2) {
      return toOrderXMLV2(enhet, innsynskrav, innsynskravDelList);
    }
    return toOrderXMLV1(enhet, innsynskrav, innsynskravDelList);
  }

  static String toOrderXMLV1(Enhet enhet, Innsynskrav innsynskrav,
      List<InnsynskravDel> innsynskravDelList) {
    return XML.toString(toOrderJSONV1(enhet, innsynskrav, innsynskravDelList), 2);
  }


  static String toOrderXMLV2(Enhet enhet, Innsynskrav innsynskrav,
      List<InnsynskravDel> innsynskravDelList) {
    return XML.toString(toOrderJSONV1(enhet, innsynskrav, innsynskravDelList), 2);
  }


  /**
   * Generate a order JSON for each Enhet
   * 
   * @param innsynskrav
   * @return
   */
  static JSONObject toOrderJSONV1(Enhet enhet, Innsynskrav innsynskrav,
      List<InnsynskravDel> innsynskravDelList) {
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

    // @formatter:off
    var bestilling = new JSONObject()
      .put("id", innsynskrav.getId())
      .put("bestillingsdato", innsynskrav.getOpprettetDato())
      .put("til", new JSONObject()
        .put("enhet", enhet.getNavn())
        .put("enhetepost", enhet.getKontaktpunktEpost())
      )
      .put("innsynskravepost", enhet.getInnsynskravEpost())
      .put("kontaktinfo", new JSONObject()
        .put("forsendelsesmåte", "e-post")
        .put("e-post", innsynskrav.getEpost())
        // .put("navn", innsynskrav.getNavn())
        // .put("organisasjon", innsynskrav.getOrganisasjon())
        // .put("land", innsynskrav.getLand())
      )
      .put("dokumenter", new ArrayList<JSONObject>());
    // @formatter:on

    innsynskravDelList.forEach(innsynskravDel -> {
      // Generate saksnummer
      var journalpost = innsynskravDel.getJournalpost();
      var saksmappe = journalpost.getSaksmappe();
      var saksaar = saksmappe.getSaksaar();
      var saksnummer =
          (saksaar > 100 ? saksaar : saksaar + 1900) + "/" + saksmappe.getSakssekvensnummer();

      // Add this document to "dokumenter" list
      var dokumenter = bestilling.getJSONArray("dokumenter");
      // @formatter:off
      dokumenter.put(new JSONObject()
        .put("dokument", new JSONObject()
          .put("saksnr", saksnummer)
          .put("dokumentnr", journalpost.getJournalpostnummer())
          .put("journalnr", journalpost.getJournalsekvensnummer())
          .put("saksbehandler", journalpost.getSaksbehandler())
        )
      );
      // @formatter:on

    });

    var orderFile = new JSONObject().put("bestilling", bestilling);
    return orderFile;
  }


  public JSONObject toOrderJSONV2(Enhet enhet, Innsynskrav innsynskrav) {
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

    // @formatter:off
    var bestilling = new JSONObject()
      .put("id", innsynskrav.getId())
      .put("bestillingsdato", innsynskrav.getOpprettetDato())
      .put("til", new JSONObject()
        .put("virksomhet", enhet.getNavn())
        .put("orgnr", enhet.getOrgnummer())
        .put("enhetepost", enhet.getKontaktpunktEpost())
        .put("innsynskravepost", enhet.getInnsynskravEpost())
      )
      .put("kontaktinfo", new JSONObject()
        .put("forsendelsesmåte", "e-post")
        .put("e-post", innsynskrav.getEpost())
      )
      .put("dokumenter", new ArrayList<JSONObject>());
    // @formatter:on

    var innsynskravDelList = innsynskrav.getInnsynskravDel();
    innsynskravDelList.forEach(innsynskravDel -> {
      // Generate saksnummer
      var journalpost = innsynskravDel.getJournalpost();
      var saksmappe = journalpost.getSaksmappe();
      var saksaar = saksmappe.getSaksaar();
      var saksnummer =
          (saksaar > 100 ? saksaar : saksaar + 1900) + "/" + saksmappe.getSakssekvensnummer();

      // Add this document to "dokumenter" list
      var dokumenter = bestilling.getJSONArray("dokumenter");
      // @formatter:off
      dokumenter.put(new JSONObject()
        .put("dokument", new JSONObject()
          .put("fagsysteminfo", "") // TODO
          .put("id", journalpost.getId())
          .put("systemId", "") // TODO
          .put("saksnr", saksnummer)
          .put("dokumentnr", journalpost.getJournalpostnummer())
          .put("journalnr", journalpost.getJournalsekvensnummer())
          .put("saksbehandler", journalpost.getSaksbehandler())
          .put("admEnhet", journalpost.getAdministrativEnhet())
        )
      );
      // @formatter:on

    });

    var orderFile = new JSONObject().put("bestilling", bestilling);
    return orderFile;
  }

}
