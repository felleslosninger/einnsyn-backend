package no.einnsyn.apiv3.entities.search;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import no.einnsyn.apiv3.entities.search.models.SearchResultItem;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SearchResultItemSerializer implements JsonSerializer<SearchResultItem> {

  @Override
  public JsonElement serialize(
      SearchResultItem searchResultItem, Type typeOfSrc, JsonSerializationContext context) {

    var journalpostJSON = searchResultItem.getJournalpostJSON();
    if (journalpostJSON != null) {
      return context.serialize(journalpostJSON);
    }

    var saksmappeJSON = searchResultItem.getSaksmappeJSON();
    if (saksmappeJSON != null) {
      return context.serialize(saksmappeJSON);
    }

    System.err.println("Found unknown search result item");
    return null;
  }
}
