package no.einnsyn.apiv3.entities.journalpost;

import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.requests.GetListRequestParameters;

@Getter
@Setter
public class JournalpostGetListRequestParameters extends GetListRequestParameters {

  @ExistingObject(type = Saksmappe.class)
  private String saksmappeId;
}
