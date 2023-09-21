package no.einnsyn.apiv3.entities;

import java.util.Set;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;

/**
 * Interface that should be implemented for all services that handle entity objects.
 */
public interface IEinnsynEntityService<OBJECT extends EinnsynObject, JSON extends EinnsynObjectJSON>
    extends IEinnsynService<OBJECT, JSON> {

  /**
   * Update a entity from a JSON object. This should update all relevant databases, and all affected
   * children / parents.
   * 
   * @param id
   * @param json
   * @return
   */
  public JSON update(String id, JSON json);


  /**
   * A helper method that creates a new object and calls fromJSON from IEinnsynService.
   * 
   * @param json
   * @param paths A list of paths containing new objects that will be created from this update.
   * @param currentPath The current path in the object tree.
   * @return
   */
  public OBJECT fromJSON(JSON json, Set<String> paths, String currentPath);


  /**
   * A helper method that creates a new JSON object and calls toJSON from IEinnsynService.
   * 
   * @param object
   * @param expandPaths A list of paths to expand. Un-expanded objects will be shown as IDs.
   * @param currentPath The current path in the object tree.
   * @return
   */
  public JSON toJSON(OBJECT object, Set<String> expandPaths, String currentPath);


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
  public ExpandableField<JSON> maybeExpand(OBJECT object, String propertyName,
      Set<String> expandPaths, String currentPath);

}
