package no.einnsyn.apiv3.features.search;

import java.lang.reflect.Type;
import org.springframework.context.annotation.Configuration;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import no.einnsyn.apiv3.features.search.models.SearchResultItem;

@Configuration
public class SearchResultItemSerializer implements JsonSerializer<SearchResultItem> {

  // public SearchResultItemSerializer(JournalpostService journalpostService,
  // SaksmappeService saksmappeService) {
  // this.journalpostService = journalpostService;
  // this.saksmappeService = saksmappeService;
  // }

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

    System.err.println("Found unknown search result item");
    return null;
  }

}
