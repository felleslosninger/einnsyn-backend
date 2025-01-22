package no.einnsyn.backend.entities.lagretsoek.models;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.BaseES;

@Getter
@Setter
public class LagretSoekES extends BaseES {
  Map<String, Object> query;

  // Legacy
  String abonnement_type = "søk";
}
