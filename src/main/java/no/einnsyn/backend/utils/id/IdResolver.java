package no.einnsyn.backend.utils.id;

import java.util.Map;
import no.einnsyn.backend.entities.apikey.ApiKeyService;
import no.einnsyn.backend.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.backend.entities.arkiv.ArkivService;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.ArkivdelService;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.behandlingsprotokoll.BehandlingsprotokollService;
import no.einnsyn.backend.entities.behandlingsprotokoll.models.BehandlingsprotokollDTO;
import no.einnsyn.backend.entities.bruker.BrukerService;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.identifikator.IdentifikatorService;
import no.einnsyn.backend.entities.identifikator.models.IdentifikatorDTO;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravService;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravBestillingService;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.klasse.KlasseService;
import no.einnsyn.backend.entities.klasse.models.KlasseDTO;
import no.einnsyn.backend.entities.klassifikasjonssystem.KlassifikasjonssystemService;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.KlassifikasjonssystemDTO;
import no.einnsyn.backend.entities.korrespondansepart.KorrespondansepartService;
import no.einnsyn.backend.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.backend.entities.lagretsak.LagretSakService;
import no.einnsyn.backend.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekService;
import no.einnsyn.backend.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.backend.entities.mappe.MappeService;
import no.einnsyn.backend.entities.mappe.models.MappeDTO;
import no.einnsyn.backend.entities.moetedeltaker.MoetedeltakerService;
import no.einnsyn.backend.entities.moetedeltaker.models.MoetedeltakerDTO;
import no.einnsyn.backend.entities.moetedokument.MoetedokumentService;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.registrering.RegistreringService;
import no.einnsyn.backend.entities.registrering.models.RegistreringDTO;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
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
  private static final Map<Class<?>, Class<? extends BaseService>> ENTITY_SERVICE_MAP =
      Map.ofEntries(
          Map.entry(ApiKeyDTO.class, ApiKeyService.class),
          Map.entry(ArkivDTO.class, ArkivService.class),
          Map.entry(ArkivdelDTO.class, ArkivdelService.class),
          Map.entry(BehandlingsprotokollDTO.class, BehandlingsprotokollService.class),
          Map.entry(BrukerDTO.class, BrukerService.class),
          Map.entry(DokumentbeskrivelseDTO.class, DokumentbeskrivelseService.class),
          Map.entry(DokumentobjektDTO.class, DokumentobjektService.class),
          Map.entry(EnhetDTO.class, EnhetService.class),
          Map.entry(IdentifikatorDTO.class, IdentifikatorService.class),
          Map.entry(InnsynskravDTO.class, InnsynskravService.class),
          Map.entry(InnsynskravBestillingDTO.class, InnsynskravBestillingService.class),
          Map.entry(JournalpostDTO.class, JournalpostService.class),
          Map.entry(KlasseDTO.class, KlasseService.class),
          Map.entry(KlassifikasjonssystemDTO.class, KlassifikasjonssystemService.class),
          Map.entry(KorrespondansepartDTO.class, KorrespondansepartService.class),
          Map.entry(LagretSakDTO.class, LagretSakService.class),
          Map.entry(LagretSoekDTO.class, LagretSoekService.class),
          Map.entry(MappeDTO.class, MappeService.class),
          Map.entry(MoetedeltakerDTO.class, MoetedeltakerService.class),
          Map.entry(MoetedokumentDTO.class, MoetedokumentService.class),
          Map.entry(MoetemappeDTO.class, MoetemappeService.class),
          Map.entry(RegistreringDTO.class, RegistreringService.class),
          Map.entry(SaksmappeDTO.class, SaksmappeService.class));

  public IdResolver(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * Resolves an input identifier to its canonical eInnsyn ID for a specific entity type.
   *
   * <p>If the input is already an eInnsyn ID (has a recognized entity prefix), it is returned
   * unchanged. Otherwise, the method attempts to resolve it through the specified entity service
   * using its resolveId() method, which handles entity-specific identifier formats.
   *
   * @param inputId the identifier to resolve (can be system ID, email, orgnummer, UUID, etc.)
   * @param entityClass the entity class to determine which service to use for resolution
   * @return the canonical eInnsyn ID if resolution succeeds, otherwise the original input ID
   */
  public String resolveToEInnsynId(String inputId, Class<?> entityClass) {
    if (inputId == null || inputId.isEmpty()) {
      return inputId;
    }

    // Check if it's already an eInnsyn ID
    var entityType = IdUtils.resolveEntity(inputId);
    if (entityType != null) {
      return inputId;
    }

    // Get the appropriate service for this entity class
    var serviceClass = ENTITY_SERVICE_MAP.get(entityClass);
    if (serviceClass == null) {
      return inputId;
    }

    try {
      var service = applicationContext.getBean(serviceClass);
      var resolvedId = service.resolveId(inputId);
      if (resolvedId != null) {
        return resolvedId;
      }
    } catch (Exception e) {
      // Return original ID if resolution fails
    }

    // Return original ID if no resolution was possible
    return inputId;
  }
}
