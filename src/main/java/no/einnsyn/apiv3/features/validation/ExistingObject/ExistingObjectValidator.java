package no.einnsyn.apiv3.features.validation.ExistingObject;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.enhet.EnhetRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;

public class ExistingObjectValidator
    implements ConstraintValidator<ExistingObject, ExpandableField<?>> {

  private Class<? extends EinnsynObject> clazz;

  private final EnhetRepository enhetRepository;
  private final JournalpostRepository journalpostRepository;
  private final SaksmappeRepository saksmappeRepository;

  public ExistingObjectValidator(EnhetRepository enhetRepository,
      JournalpostRepository journalpostRepository, SaksmappeRepository saksmappeRepository) {
    this.enhetRepository = enhetRepository;
    this.journalpostRepository = journalpostRepository;
    this.saksmappeRepository = saksmappeRepository;
  }

  @Override
  public void initialize(ExistingObject constraint) {
    clazz = constraint.type();
  }

  /**
   * Checks if a given ID exists in the repository for the given class.
   */
  @Override
  public boolean isValid(ExpandableField<?> field, ConstraintValidatorContext cxt) {
    // If no value is given, we regard it as valid.
    if (field == null) {
      return true;
    }

    // Check if object exists in DB
    String id = field.getId();
    if (id != null) {
      if (clazz == Enhet.class) {
        return enhetRepository.existsById(id);
      } else if (clazz == Journalpost.class) {
        return journalpostRepository.existsById(id);
      } else if (clazz == Saksmappe.class) {
        return saksmappeRepository.existsById(id);
      }
    }

    return false;
  }
}
