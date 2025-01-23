package no.einnsyn.backend.entities.lagretsoek.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LegacyQuery {
  private String searchTerm = "";
  private List<SearchTerm> searchTerms = new ArrayList<>();
  private List<QueryFilter> appliedFilters = new ArrayList<>();
  private Map<String, QueryAggregation> aggregations = new HashMap<>();
  private List<String> ids;
  private Sort sort = new Sort();
  private int offset = 0;
  private int size = 20;

  @Getter
  @Setter
  public class QueryFilter {
    private String fieldName;
    private String type;

    @Getter
    @Setter
    public class NotQueryFilter extends QueryFilter {
      private Collection<String> fieldValue;
    }

    @Getter
    @Setter
    public class PostQueryFilter extends QueryFilter {
      private Collection<String> fieldValue;
    }

    @Getter
    @Setter
    public class RangeQueryFilter extends QueryFilter {
      private String from;
      private String to;
    }

    @Getter
    @Setter
    public class TermQueryFilter extends QueryFilter {
      private Collection<String> fieldValue;
    }
  }

  @Getter
  @Setter
  public class Sort {

    private final String defaultSortFieldName = "_score";
    private SortOrder order;

    // @Schema(
    //     allowableValues = {
    //       "standardDato, publisertDato, journaldato, dokumentetsDato, opprettetDato, moetedato,"
    //           + " journalpostnummer, search_saksaar, search_sakssekvensnummer,
    // m\u00f8tesaks\u00e5r,"
    //           + " m\u00f8tesakssekvensnummer, search_tittel_sort, arkivskaperSorteringNavn,"
    //           + " journalposttype, search_korrespodansepart_sort, sakssekvensnummer_sort,"
    //           + " journalpostnummer_sort, _score"
    //     })
    private String fieldName;

    public enum SortOrder {
      ASC,
      DESC;

      private SortOrder() {}
    }
  }

  @Getter
  @Setter
  public class SearchTerm {
    private String searchTerm;

    //  @Schema(
    //     allowableValues = {"search_id, search_tittel, search_innhold,
    // korrespondansepart.korrespondansepartNavn, skjerming.skjermingshjemmel, avsender, mottaker,
    // journalpostnummer, search_saksaar, search_sakssekvensnummer, m\u00f8tesaks\u00e5r,
    // m\u00f8tesakssekvensnummer"}
    //  )
    private String field;
    private Operator operator;

    public enum Operator {
      PHRASE,
      AND,
      OR,
      NOT_ANY,
      SIMPLE_QUERY_STRING
    }
  }

  @Getter
  @Setter
  public class QueryAggregation {
    // @Schema(allowableValues = {"arkivskaperTransitive, type"})
    private String fieldName;
  }
}
