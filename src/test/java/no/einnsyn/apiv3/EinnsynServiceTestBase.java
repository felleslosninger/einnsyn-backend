package no.einnsyn.apiv3;

import java.time.LocalDate;
import java.util.List;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.behandlingsprotokoll.models.BehandlingsprotokollDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.entities.moetedeltaker.models.MoetedeltakerDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingDTO;
import no.einnsyn.apiv3.entities.utredning.models.UtredningDTO;
import no.einnsyn.apiv3.entities.vedtak.models.VedtakDTO;
import no.einnsyn.apiv3.entities.votering.models.VoteringDTO;

public abstract class EinnsynServiceTestBase extends EinnsynTestBase {

  protected SaksmappeDTO getSaksmappeDTO() {
    var saksmappeDTO = new SaksmappeDTO();
    saksmappeDTO = new SaksmappeDTO();
    saksmappeDTO.setOffentligTittel("test 1");
    saksmappeDTO.setOffentligTittelSensitiv("test 1 sensitiv");
    saksmappeDTO.setBeskrivelse("test 1 beskrivelse");
    saksmappeDTO.setSaksaar(2023);
    saksmappeDTO.setSakssekvensnummer(1);
    return saksmappeDTO;
  }

  protected JournalpostDTO getJournalpostDTO() {
    var journalpostDTO = new JournalpostDTO();
    journalpostDTO.setOffentligTittel("test 1");
    journalpostDTO.setOffentligTittelSensitiv("test 1 sensitiv");
    journalpostDTO.setJournalaar(2023);
    journalpostDTO.setJournalsekvensnummer(1);
    journalpostDTO.setJournalposttype("innkommende_dokument");
    journalpostDTO.setJournaldato(LocalDate.of(2023, 1, 2).toString());
    journalpostDTO.setJournalpostnummer(1);
    journalpostDTO.setDokumentetsDato("2023-01-02");
    return journalpostDTO;
  }

  protected KorrespondansepartDTO getKorrespondansepartDTO() {
    var korrespondansepartDTO = new KorrespondansepartDTO();
    korrespondansepartDTO.setKorrespondansepartNavn("test 1");
    korrespondansepartDTO.setKorrespondansepartNavnSensitiv("test 1 sensitiv");
    korrespondansepartDTO.setKorrespondanseparttype("mottaker");
    korrespondansepartDTO.setEpostadresse("epost1@example.com");
    korrespondansepartDTO.setAdministrativEnhet("https://testAdmEnhet1");
    return korrespondansepartDTO;
  }

  protected DokumentbeskrivelseDTO getDokumentbeskrivelseDTO() {
    var dokumentbeskrivelseDTO = new DokumentbeskrivelseDTO();
    dokumentbeskrivelseDTO.setTittel("dokumentbeskrivelsetest 1");
    dokumentbeskrivelseDTO.setTittelSensitiv("dokumentbeskrivelsetest 1 sensitiv");
    dokumentbeskrivelseDTO.setTilknyttetRegistreringSom("vedlegg");
    dokumentbeskrivelseDTO.setDokumentobjekt(
        List.of(
            new ExpandableField<>(getDokumentobjektDTO()),
            new ExpandableField<>(getDokumentobjektDTO())));
    return dokumentbeskrivelseDTO;
  }

  protected DokumentobjektDTO getDokumentobjektDTO() {
    var dokumentobjektDTO = new DokumentobjektDTO();
    dokumentobjektDTO.setReferanseDokumentfil("https://example.com");
    dokumentobjektDTO.setFormat("pdf");
    dokumentobjektDTO.setSjekksum("123");
    dokumentobjektDTO.setSjekksumAlgoritme("MD5");
    return dokumentobjektDTO;
  }

  protected SkjermingDTO getSkjermingDTO() {
    var skjermingDTO = new SkjermingDTO();
    skjermingDTO.setSkjermingshjemmel("offl. § 13");
    skjermingDTO.setTilgangsrestriksjon("foo");
    return skjermingDTO;
  }

  private int moetenummerIterator = 1;

  protected MoetemappeDTO getMoetemappeDTO() {
    var moetemappeDTO = new MoetemappeDTO();
    moetemappeDTO.setOffentligTittel("Møtemappe, offentlig tittel");
    moetemappeDTO.setOffentligTittelSensitiv("Møtemappe, offentlig tittel sensitiv");
    moetemappeDTO.setMoetenummer(String.valueOf(moetenummerIterator++));
    moetemappeDTO.setUtvalg("utvalg");
    moetemappeDTO.setMoetedato("2020-01-01T00:00:00Z");
    moetemappeDTO.setMoetested("moetested");
    moetemappeDTO.setVideoLink("https://example.com/video");
    moetemappeDTO.setMoetedokument(
        List.of(
            new ExpandableField<>(getMoetedokumentDTO()),
            new ExpandableField<>(getMoetedokumentDTO()),
            new ExpandableField<>(getMoetedokumentDTO())));
    moetemappeDTO.setMoetesak(
        List.of(
            new ExpandableField<>(getMoetesakDTO()),
            new ExpandableField<>(getMoetesakDTO()),
            new ExpandableField<>(getMoetesakDTO())));
    return moetemappeDTO;
  }

  protected MoetedokumentDTO getMoetedokumentDTO() {
    var moetedokumentDTO = new MoetedokumentDTO();
    moetedokumentDTO.setOffentligTittel("Møtedokument, offentlig tittel");
    moetedokumentDTO.setOffentligTittelSensitiv("Møtedokument, offentlig tittel sensitiv");
    moetedokumentDTO.setBeskrivelse("beskrivelse");
    moetedokumentDTO.setMoetedokumenttype("saksliste");
    moetedokumentDTO.setKorrespondansepart(
        List.of(
            new ExpandableField<>(getKorrespondansepartDTO()),
            new ExpandableField<>(getKorrespondansepartDTO())));
    moetedokumentDTO.setDokumentbeskrivelse(
        List.of(
            new ExpandableField<>(getDokumentbeskrivelseDTO()),
            new ExpandableField<>(getDokumentbeskrivelseDTO())));
    return moetedokumentDTO;
  }

  protected MoetesakDTO getMoetesakDTO() {
    var moetesakDTO = new MoetesakDTO();
    moetesakDTO.setOffentligTittel("Møtesak, offentlig tittel");
    moetesakDTO.setOffentligTittelSensitiv("Møtesak, offentlig tittel sensitiv");
    moetesakDTO.setMoetesakstype("moete");
    moetesakDTO.setMoetesaksaar(2020);
    moetesakDTO.setMoetesakssekvensnummer(1);
    moetesakDTO.setUtvalg("enhet");
    moetesakDTO.setVideoLink("https://example.com/video");
    moetesakDTO.setUtredning(new ExpandableField<>(getUtredningDTO()));
    moetesakDTO.setVedtak(new ExpandableField<>(getVedtakDTO()));
    moetesakDTO.setInnstilling(new ExpandableField<>(getMoetesaksbeskrivelseDTO()));
    return moetesakDTO;
  }

  protected UtredningDTO getUtredningDTO() {
    var utredningDTO = new UtredningDTO();
    utredningDTO.setSaksbeskrivelse(new ExpandableField<>(getMoetesaksbeskrivelseDTO()));
    utredningDTO.setInnstilling(new ExpandableField<>(getMoetesaksbeskrivelseDTO()));
    utredningDTO.setUtredningsdokument(
        List.of(
            new ExpandableField<>(getDokumentbeskrivelseDTO()),
            new ExpandableField<>(getDokumentbeskrivelseDTO())));
    return utredningDTO;
  }

  protected VedtakDTO getVedtakDTO() {
    var vedtakDTO = new VedtakDTO();
    vedtakDTO.setDato("2020-01-01");
    vedtakDTO.setVedtakstekst(new ExpandableField<>(getMoetesaksbeskrivelseDTO()));
    vedtakDTO.setBehandlingsprotokoll(new ExpandableField<>(getBehandlingsprotokollDTO()));
    vedtakDTO.setVotering(
        List.of(
            new ExpandableField<>(getVoteringDTO()),
            new ExpandableField<>(getVoteringDTO()),
            new ExpandableField<>(getVoteringDTO())));
    vedtakDTO.setVedtaksdokument(
        List.of(
            new ExpandableField<>(getDokumentbeskrivelseDTO()),
            new ExpandableField<>(getDokumentbeskrivelseDTO())));
    return vedtakDTO;
  }

  protected MoetesaksbeskrivelseDTO getMoetesaksbeskrivelseDTO() {
    var moetesaksbeskrivelseDTO = new MoetesaksbeskrivelseDTO();
    moetesaksbeskrivelseDTO.setTekstInnhold("tekstInnhold");
    moetesaksbeskrivelseDTO.setTekstFormat("tekstFormat");
    return moetesaksbeskrivelseDTO;
  }

  protected BehandlingsprotokollDTO getBehandlingsprotokollDTO() {
    var behandlingsprotokollDTO = new BehandlingsprotokollDTO();
    behandlingsprotokollDTO.setTekstInnhold("tekstInnhold");
    behandlingsprotokollDTO.setTekstFormat("tekstFormat");
    return behandlingsprotokollDTO;
  }

  private int stemmeCounter = 0;

  protected VoteringDTO getVoteringDTO() {
    var mod = ++stemmeCounter % 3;
    var voteringDTO = new VoteringDTO();
    voteringDTO.setMoetedeltaker(new ExpandableField<>(getMoetedeltakerDTO()));
    voteringDTO.setStemme(mod == 0 ? "Ja" : mod == 1 ? "Nei" : "Blankt");
    return voteringDTO;
  }

  protected MoetedeltakerDTO getMoetedeltakerDTO() {
    var moetedeltakerDTO = new MoetedeltakerDTO();
    moetedeltakerDTO.setMoetedeltakerNavn("navn");
    moetedeltakerDTO.setMoetedeltakerFunksjon("funksjon");
    return moetedeltakerDTO;
  }
}
