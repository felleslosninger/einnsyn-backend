package no.einnsyn.apiv3.entities;

import java.util.Set;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;

/**
 * Interface that should be implemented for all EinnsynObject services, both for abstract
 * superclasses, and for entities.
 */
public interface IEinnsynService<OBJECT extends EinnsynObject, JSON extends EinnsynObjectJSON> {

  /**
   * Update a database entity from a JSON object. This should keep a list of objects that are
   * inserted, so we can make sure they are expanded in the resulting JSON object. The list of
   * inserted objects are represented as "paths" in the JSON object, to be passed on to toJSON
   * later.
   * 
   * @param json
   * @param object The database object to be modified.
   * @param paths A list of paths containing new objects that will be created from this update.
   * @param currentPath The current path in the object tree.
   * @return
   */
  public OBJECT fromJSON(JSON json, OBJECT object, Set<String> paths, String currentPath);


  /**
   * Convert a database entity object to a JSON object
   * 
   * @param object
   * @param json
   * @param expandPaths A list of paths to expand. Un-expanded objects will be shown as IDs.
   * @param currentPath The current path in the object tree.
   * @return
   */
  public JSON toJSON(OBJECT object, JSON json, Set<String> expandPaths, String currentPath);

}
