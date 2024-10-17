package no.einnsyn.apiv3.testutils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ElasticsearchMocks {

  /**
   * Helper function to create a mock Elasticsearch response with <size> hits, where <idList> is
   * included in the response.
   *
   * @param size
   * @param idList
   * @return
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static SearchResponse<Object> searchResponse(int size, List<String> idList) {
    var searchResponse = mock(SearchResponse.class);
    var hitsMetadata = mock(HitsMetadata.class);
    var hits = new ArrayList<Hit>();

    // Add <size> dummy hits
    for (var j = 0; j < size - idList.size(); j++) {
      var id =
          new Random()
              .ints(97, 123)
              .limit(8)
              .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
              .toString();
      var hit = mock(Hit.class);
      when(hit.id()).thenReturn("id_" + id);
      hits.add(hit);
    }

    // Add existing list
    for (var i = 0; i < idList.size() && i < size; i++) {
      var hit = mock(Hit.class);
      when(hit.id()).thenReturn(idList.get(i));
      hits.add(hit);
    }

    when(searchResponse.hits()).thenReturn(hitsMetadata);
    when(hitsMetadata.hits()).thenReturn(hits);
    return searchResponse;
  }
}
