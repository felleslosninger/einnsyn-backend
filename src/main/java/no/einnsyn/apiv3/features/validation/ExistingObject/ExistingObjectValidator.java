package no.einnsyn.apiv3.features.validation.ExistingObject;

import java.util.List;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.enhet.EnhetRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.korrespondansepart.KorrespondansepartRepository;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;

public class ExistingObjectValidator implements ConstraintValidator<ExistingObject, Object> {

  private Class<? extends Object> clazz;

  private final EnhetRepository enhetRepository;
  private final JournalpostRepository journalpostRepository;
  private final SaksmappeRepository saksmappeRepository;
  private final DokumentbeskrivelseRepository dokumentbeskrivelseRepository;
  private final KorrespondansepartRepository korrespondansepartRepository;

  public ExistingObjectValidator(EnhetRepository enhetRepository,
      JournalpostRepository journalpostRepository, SaksmappeRepository saksmappeRepository,
      DokumentbeskrivelseRepository dokumentbeskrivelseRepository,
      KorrespondansepartRepository korrespondansepartRepository) {
    this.enhetRepository = enhetRepository;
    this.journalpostRepository = journalpostRepository;
    this.saksmappeRepository = saksmappeRepository;
    this.dokumentbeskrivelseRepository = dokumentbeskrivelseRepository;
    this.korrespondansepartRepository = korrespondansepartRepository;
  }

  @Override
  public void initialize(ExistingObject constraint) {
    clazz = constraint.type();
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
        if (clazz == Enhet.class) {
          return enhetRepository.existsById(id);
        } else if (clazz == Journalpost.class) {
          return journalpostRepository.existsById(id);
        } else if (clazz == Saksmappe.class) {
          return saksmappeRepository.existsById(id);
        } else if (clazz == Dokumentbeskrivelse.class) {
          return dokumentbeskrivelseRepository.existsById(id);
        } else if (clazz == Korrespondansepart.class) {
          return korrespondansepartRepository.existsById(id);
        }
      }
    }

    return false;
  }

}
