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
   * Update a Skjerming object from a JSON object
   * 
   * @param json
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  public Skjerming fromJSON(SkjermingJSON json, Set<String> paths, String currentPath) {
    return fromJSON(json, new Skjerming(), paths, currentPath);
  }

  /**
   * Update a Skjerming object from a JSON object
   * 
   * @param json
   * @param skjerming
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
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
   * Convert a Skjerming object to a JSON object
   * 
   * @param skjerming
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public SkjermingJSON toJSON(Skjerming skjerming, Set<String> expandPaths, String currentPath) {
    return toJSON(skjerming, new SkjermingJSON(), expandPaths, currentPath);
  }

  /**
   * Convert a Skjerming object to a JSON object
   * 
   * @param skjerming
   * @param json
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
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


  /**
   * Creates an ExpandableField object. If propertyName is in the expandPaths list, the object will
   * be expanded, if not, it will only contain the ID.
   * 
   * @param skjerming
   * @param propertyName Name of the property to expand, appended to currentPath for deeper steps
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
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
