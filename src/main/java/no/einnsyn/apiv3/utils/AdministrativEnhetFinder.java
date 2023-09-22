package no.einnsyn.apiv3.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;

public class AdministrativEnhetFinder {

  /**
   * Search the subtree under `root` for an enhet with matching enhetskode. Searching breadth-first
   * to avoid unnecessary DB queries.
   * 
   * @param enhetskode
   * @param root
   * @return
   */
  public static Enhet find(String enhetskode, Enhet root) {

    // Empty string is not a valid enhetskode
    if (enhetskode == null || root == null || enhetskode.equals("")) {
      return null;
    }

    Integer checkElementCount = 0;
    Integer queryChildrenCount = 0;
    List<Enhet> queue = new ArrayList<Enhet>();
    Set<Enhet> visited = new HashSet<Enhet>();

    // Search for enhet with matching enhetskode, breadth-first to avoid unnecessary DB queries
    queue.add(root);
    while (checkElementCount < queue.size()) {
      Enhet enhet = queue.get(checkElementCount);
      checkElementCount++;

      // Avoid infinite loops
      if (visited.contains(enhet)) {
        continue;
      }
      visited.add(enhet);

      if (enhetskode.equals(enhet.getEnhetskode())) {
        return enhet;
      }

      // Add more children to queue when needed
      while (checkElementCount >= queue.size() && queryChildrenCount < queue.size()) {
        Enhet querier = queue.get(queryChildrenCount);
        queryChildrenCount++;
        List<Enhet> underenheter = querier.getUnderenheter();
        if (underenheter != null) {
          queue.addAll(underenheter);
        }
      }
    }

    return null;
  }

}
