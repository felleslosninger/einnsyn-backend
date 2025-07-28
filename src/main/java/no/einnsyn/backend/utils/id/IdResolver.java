package no.einnsyn.backend.utils.id;

import java.util.Map;
import no.einnsyn.backend.entities.apikey.ApiKeyService;
import no.einnsyn.backend.entities.arkiv.ArkivService;
import no.einnsyn.backend.entities.arkivdel.ArkivdelService;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.behandlingsprotokoll.BehandlingsprotokollService;
import no.einnsyn.backend.entities.bruker.BrukerService;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.backend.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.identifikator.IdentifikatorService;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravService;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravBestillingService;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.klasse.KlasseService;
import no.einnsyn.backend.entities.klassifikasjonssystem.KlassifikasjonssystemService;
import no.einnsyn.backend.entities.korrespondansepart.KorrespondansepartService;
import no.einnsyn.backend.entities.lagretsak.LagretSakService;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekService;
import no.einnsyn.backend.entities.mappe.MappeService;
import no.einnsyn.backend.entities.moetedeltaker.MoetedeltakerService;
import no.einnsyn.backend.entities.moetedokument.MoetedokumentService;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.registrering.RegistreringService;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.utils.idgenerator.IdUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Service responsible for resolving various identifier formats (system IDs, emails, organization
 * numbers, etc.) to their canonical eInnsyn IDs.
 *
 * <p>This resolver uses the entity service layer to perform ID lookups and conversions, supporting
 * different identifier formats for each entity type: - Bruker: email addresses - Enhet:
 * organization numbers (orgnummer) - Mappe: system IDs - Other entities: their respective
 * alternative identifier formats
 */
@Component
public class IdResolver {

  private final ApplicationContext applicationContext;

  @SuppressWarnings({"rawtypes"})
  private static final Map<String, Class<? extends BaseService>> ENTITY_SERVICE_MAP =
      Map.ofEntries(
          Map.entry("ApiKey", ApiKeyService.class),
          Map.entry("Arkiv", ArkivService.class),
          Map.entry("Arkivdel", ArkivdelService.class),
          Map.entry("Behandlingsprotokoll", BehandlingsprotokollService.class),
          Map.entry("Bruker", BrukerService.class),
          Map.entry("Dokumentbeskrivelse", DokumentbeskrivelseService.class),
          Map.entry("Dokumentobjekt", DokumentobjektService.class),
          Map.entry("Enhet", EnhetService.class),
          Map.entry("Identifikator", IdentifikatorService.class),
          Map.entry("Innsynskrav", InnsynskravService.class),
          Map.entry("InnsynskravBestilling", InnsynskravBestillingService.class),
          Map.entry("Journalpost", JournalpostService.class),
          Map.entry("Klasse", KlasseService.class),
          Map.entry("Klassifikasjonssystem", KlassifikasjonssystemService.class),
          Map.entry("Korrespondansepart", KorrespondansepartService.class),
          Map.entry("LagretSak", LagretSakService.class),
          Map.entry("LagretSoek", LagretSoekService.class),
          Map.entry("Mappe", MappeService.class),
          Map.entry("Moetedeltaker", MoetedeltakerService.class),
          Map.entry("Moetedokument", MoetedokumentService.class),
          Map.entry("Moetemappe", MoetemappeService.class),
          Map.entry("Registrering", RegistreringService.class),
          Map.entry("Saksmappe", SaksmappeService.class));

  public IdResolver(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * Resolves an input identifier to its canonical eInnsyn ID.
   *
   * <p>If the input is already an eInnsyn ID (has a recognized entity prefix), it is returned
   * unchanged. Otherwise, the method attempts to resolve it through all registered entity services
   * using their respective resolveId() methods, which handle entity-specific identifier formats.
   *
   * @param inputId the identifier to resolve (can be system ID, email, orgnummer, UUID, etc.)
   * @return the canonical eInnsyn ID if resolution succeeds, otherwise the original input ID
   */
  public String resolveToEInnsynId(String inputId) {
    if (inputId == null || inputId.isEmpty()) {
      return inputId;
    }

    // Check if it's already an eInnsyn ID
    var entityType = IdUtils.resolveEntity(inputId);
    if (entityType != null) {
      return inputId;
    }

    // Try to resolve through all entity services
    for (var entry : ENTITY_SERVICE_MAP.entrySet()) {
      try {
        var service = applicationContext.getBean(entry.getValue());
        var resolvedId = service.resolveId(inputId);
        if (resolvedId != null) {
          return resolvedId;
        }
      } catch (Exception e) {
        // Continue trying other services
      }
    }

    // Return original ID if no resolution was possible
    return inputId;
  }
}
