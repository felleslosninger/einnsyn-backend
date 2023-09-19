package no.einnsyn.apiv3.entities.skjerming;

import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.skjerming.models.Skjerming;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingJSON;

@Service
public class SkjermingService {

  private final EinnsynObjectService einnsynObjectService;

  public SkjermingService(EinnsynObjectService eInnsynObjectService) {
    this.einnsynObjectService = eInnsynObjectService;
  }


  /**
   * 
   * @param json
   * @return
   */
  public Skjerming fromJSON(SkjermingJSON json) {
    return fromJSON(new Skjerming(), json);
  }

  public Skjerming fromJSON(Skjerming skjerming, SkjermingJSON json) {
    einnsynObjectService.fromJSON(skjerming, json);

    if (json.getTilgangsrestriksjon() != null) {
      skjerming.setTilgangsrestriksjon(json.getTilgangsrestriksjon());
    }

    if (json.getSkjermingshjemmel() != null) {
      skjerming.setSkjermingshjemmel(json.getSkjermingshjemmel());
    }

    return skjerming;
  }


  /**
   * 
   * @param skjerming
   * @param depth
   * @return
   */
  public SkjermingJSON toJSON(Skjerming skjerming, Integer depth) {
    return toJSON(skjerming, new SkjermingJSON(), depth);
  }

  public SkjermingJSON toJSON(Skjerming skjerming, SkjermingJSON json, Integer depth) {
    einnsynObjectService.toJSON(skjerming, json, depth);

    if (skjerming.getTilgangsrestriksjon() != null) {
      json.setTilgangsrestriksjon(skjerming.getTilgangsrestriksjon());
    }

    if (skjerming.getSkjermingshjemmel() != null) {
      json.setSkjermingshjemmel(skjerming.getSkjermingshjemmel());
    }

    return json;
  }
}
