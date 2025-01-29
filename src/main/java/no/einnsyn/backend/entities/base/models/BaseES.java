package no.einnsyn.backend.entities.base.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseES {
  private String id;

  private String externalId;

  // Legacy. An array with a single string, the type of the object
  private List<String> type;
}
