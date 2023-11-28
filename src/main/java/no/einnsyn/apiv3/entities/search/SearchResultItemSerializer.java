package no.einnsyn.apiv3.entities.search;

import java.lang.reflect.Type;
import org.springframework.context.annotation.Configuration;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.search.models.SearchResultItem;

@Slf4j
@Configuration
public class SearchResultItemSerializer implements JsonSerializer<SearchResultItem> {

  @Override
  public JsonElement serialize(SearchResultItem searchResultItem, Type typeOfSrc,
      JsonSerializationContext context) {

    var journalpostJSON = searchResultItem.getJournalpostJSON();
    if (journalpostJSON != null) {
      return context.serialize(journalpostJSON);
    }

    var saksmappeJSON = searchResultItem.getSaksmappeJSON();
    if (saksmappeJSON != null) {
      return context.serialize(saksmappeJSON);
    }

    log.error("Found unknown search result item", searchResultItem);
    return null;
  }

}
