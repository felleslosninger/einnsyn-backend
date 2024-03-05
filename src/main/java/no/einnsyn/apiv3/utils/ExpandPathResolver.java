package no.einnsyn.apiv3.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.hasid.HasId;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class ExpandPathResolver {

  private ExpandPathResolver() {
    throw new IllegalStateException("Utility class");
  }

  public static Set<String> resolve(HasId obj) {
    return resolve(obj, new HashSet<>(), null);
  }

  public static Set<String> resolve(HasId obj, Set<String> paths, String currentPath) {

    if (currentPath != null) {
      paths.add(currentPath);
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

      var path = currentPath == null ? field.getName() : currentPath + "." + field.getName();

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
