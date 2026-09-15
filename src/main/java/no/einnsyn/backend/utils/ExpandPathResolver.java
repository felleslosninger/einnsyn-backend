package no.einnsyn.backend.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.hasid.HasId;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class ExpandPathResolver {

  private ExpandPathResolver() {}

  /**
   * Builds the dotted path of a property below a parent path, e.g. "innsynskravBestilling" below
   * "innsynskrav" gives "innsynskrav.innsynskravBestilling". This is the format used both when
   * resolving paths from a DTO and when checking whether a relation should be expanded.
   *
   * @param currentPath the path of the parent object, null or empty at the root
   * @param propertyName the name of the property on the parent
   * @return the path of the property
   */
  public static String getPath(String currentPath, String propertyName) {
    return currentPath == null || currentPath.isEmpty()
        ? propertyName
        : currentPath + "." + propertyName;
  }

  public static Set<String> resolve(HasId obj) {
    return resolve(obj, new HashSet<>(), null);
  }

  /**
   * When updating objects, we want to return all updated nested objects in the response. This
   * method will take a DTO and return a list of paths that should be expanded in the response.
   *
   * @param obj the object to resolve paths for
   * @param paths the set of paths to populate
   * @param currentPath the current path in the object hierarchy
   * @return the set of paths to expand
   */
  public static Set<String> resolve(HasId obj, Set<String> paths, String currentPath) {

    if (currentPath != null) {
      paths.add(currentPath);
    }

    if (obj == null) {
      return paths;
    }

    var objClass = obj.getClass();
    var fields = objClass.getDeclaredFields();

    // Add fields for super classes
    var superClass = objClass.getSuperclass();
    while (superClass != null) {
      fields = ArrayUtils.addAll(fields, superClass.getDeclaredFields());
      superClass = superClass.getSuperclass();
    }

    for (var field : fields) {
      Object fieldObj = null;
      try {
        var getter = obj.getClass().getMethod("get" + StringUtils.capitalize(field.getName()));
        fieldObj = getter.invoke(obj);
      } catch (Exception e) {
        continue;
      }

      var path = getPath(currentPath, field.getName());

      if (fieldObj instanceof List<?> list) {
        for (var item : list) {
          if (item instanceof ExpandableField<?> expandableField && expandableField.isExpanded()) {
            resolve(expandableField.getExpandedObject(), paths, path);
          }
        }
      } else if (fieldObj instanceof ExpandableField<?> expandableField
          && expandableField.isExpanded()) {
        resolve(expandableField.getExpandedObject(), paths, path);
      }
    }

    return paths;
  }
}
