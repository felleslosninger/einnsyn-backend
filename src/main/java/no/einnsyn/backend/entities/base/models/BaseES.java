package no.einnsyn.backend.entities.base.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseES {
  /**
   * Join-parent value for documents that statistics children (innsynskrav, download) attach to.
   *
   * <p>The name is historical: only Registrering carried it when the join was introduced. ES join
   * relations cannot be renamed without a new index, so Mappe reuses the same name.
   */
  public static final String STAT_PARENT_RELATION = "registrering";

  private String id;

  private String externalId;

  private String created;

  private String updated;

  private String accessibleAfter;

  // Legacy. An array with a single string, the type of the object
  private List<String> type;
}
