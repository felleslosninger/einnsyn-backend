package no.einnsyn.apiv3.entities.skjerming;

import java.util.Set;
import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
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
  public Skjerming fromJSON(SkjermingJSON json, Set<String> paths, String currentPath) {
    return fromJSON(json, new Skjerming(), paths, currentPath);
  }

  public Skjerming fromJSON(SkjermingJSON json, Skjerming skjerming, Set<String> paths,
      String currentPath) {
    einnsynObjectService.fromJSON(json, skjerming, paths, currentPath);

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
  public SkjermingJSON toJSON(Skjerming skjerming, Set<String> expandPaths, String currentPath) {
    return toJSON(skjerming, new SkjermingJSON(), expandPaths, currentPath);
  }

  public SkjermingJSON toJSON(Skjerming skjerming, SkjermingJSON json, Set<String> expandPaths,
      String currentPath) {
    einnsynObjectService.toJSON(skjerming, json, expandPaths, currentPath);

    if (skjerming.getTilgangsrestriksjon() != null) {
      json.setTilgangsrestriksjon(skjerming.getTilgangsrestriksjon());
    }

    if (skjerming.getSkjermingshjemmel() != null) {
      json.setSkjermingshjemmel(skjerming.getSkjermingshjemmel());
    }

    return json;
  }


  public ExpandableField<SkjermingJSON> maybeExpand(Skjerming skjerming, String propertyName,
      Set<String> expandPaths, String currentPath) {
    if (expandPaths.contains(currentPath)) {
      return new ExpandableField<SkjermingJSON>(skjerming.getId(), this.toJSON(skjerming,
          expandPaths, currentPath == "" ? propertyName : currentPath + "." + propertyName));
    } else {
      return new ExpandableField<SkjermingJSON>(skjerming.getId(), null);
    }
  }
}
