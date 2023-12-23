package no.einnsyn.apiv3.features.validation.ExistingObject;

import java.util.List;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import no.einnsyn.apiv3.entities.bruker.BrukerService;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravService;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.korrespondansepart.KorrespondansepartService;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeService;
import no.einnsyn.apiv3.entities.tilbakemelding.TilbakemeldingService;

public class ExistingObjectValidator implements ConstraintValidator<ExistingObject, Object> {

  private EinnsynObjectService<? extends EinnsynObject, ? extends EinnsynObjectJSON> service;

  private final EnhetService enhetService;
  private final JournalpostService journalpostService;
  private final SaksmappeService saksmappeService;
  private final DokumentbeskrivelseService dokumentbeskrivelseService;
  private final KorrespondansepartService korrespondansepartService;
  private final InnsynskravService innsynskravService;
  private final BrukerService brukerService;
  private final TilbakemeldingService  tilbakemeldingService;

  public ExistingObjectValidator(EnhetService enhetService, JournalpostService journalpostService,
      SaksmappeService saksmappeService, DokumentbeskrivelseService dokumentbeskrivelseService,
      KorrespondansepartService korrespondansepartService, InnsynskravService innsynskravService,
      BrukerService brukerService, TilbakemeldingService tilbakemeldingService) {
    this.enhetService = enhetService;
    this.journalpostService = journalpostService;
    this.saksmappeService = saksmappeService;
    this.dokumentbeskrivelseService = dokumentbeskrivelseService;
    this.korrespondansepartService = korrespondansepartService;
    this.innsynskravService = innsynskravService;
    this.brukerService = brukerService;
    this.tilbakemeldingService = tilbakemeldingService;
  }

  @Override
  public void initialize(ExistingObject constraint) {
    var clazz = constraint.type();

    switch (clazz.getSimpleName()) {
      case "Enhet":
        service = enhetService;
        break;
      case "Journalpost":
        service = journalpostService;
        break;
      case "Saksmappe":
        service = saksmappeService;
        break;
      case "Dokumentbeskrivelse":
        service = dokumentbeskrivelseService;
        break;
      case "Korrespondansepart":
        service = korrespondansepartService;
        break;
      case "Innsynskrav":
        service = innsynskravService;
        break;
      case "Bruker":
        service = brukerService;
        break;
      case "Tilbakemelding":
        service = tilbakemeldingService;
        break;
      default:
        throw new IllegalArgumentException("Unknown type: " + clazz.getSimpleName());
    }
  }

  /**
   * Checks if a given ID exists in the repository for the given class.
   */
  @Override
  public boolean isValid(Object unknownObject, ConstraintValidatorContext cxt) {
    // If no value is given, we regard it as valid.
    if (unknownObject == null) {
      return true;
    }

    // If we have a list, we check if all elements are valid
    if (unknownObject instanceof List) {
      for (Object o : (List<?>) unknownObject) {
        if (!isValid(o, cxt)) {
          return false;
        }
      }
      return true;
    }

    // We have a String (id) or ExpandableField
    if (unknownObject instanceof ExpandableField || unknownObject instanceof String) {
      String id;
      if (unknownObject instanceof ExpandableField) {
        ExpandableField<?> field = (ExpandableField<?>) unknownObject;
        id = field.getId();
      } else {
        id = (String) unknownObject;
      }

      // Check if object exists in DB
      if (id != null) {
        return service.existsById(id);
      }
    }

    return false;
  }

}
