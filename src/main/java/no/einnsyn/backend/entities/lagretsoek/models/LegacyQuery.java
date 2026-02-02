package no.einnsyn.backend.entities.lagretsoek.models;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = QueryFilter.NotQueryFilter.class, name = "notQueryFilter"),
    @JsonSubTypes.Type(value = QueryFilter.PostQueryFilter.class, name = "postQueryFilter"),
    @JsonSubTypes.Type(value = QueryFilter.RangeQueryFilter.class, name = "rangeQueryFilter"),
    @JsonSubTypes.Type(value = QueryFilter.TermQueryFilter.class, name = "termQueryFilter")
  })
  public static class QueryFilter {
    private String fieldName;
    private String type;

    @Getter
    @Setter
    public static class NotQueryFilter extends QueryFilter {
      private Collection<String> fieldValue;
    }

    @Getter
    @Setter
    public static class PostQueryFilter extends QueryFilter {
      private Collection<String> fieldValue;
    }

    @Getter
    @Setter
    public static class RangeQueryFilter extends QueryFilter {
      private String from;
      private String to;
    }

    @Getter
    @Setter
    public static class TermQueryFilter extends QueryFilter {
      private Collection<String> fieldValue;
    }
  }

  @Getter
  @Setter
  public static class Sort {

    private final String defaultSortFieldName = "_score";
    private SortOrder order;

    private String fieldName;

    public enum SortOrder {
      ASC,
      DESC;

      private SortOrder() {}
    }
  }

  @Getter
  @Setter
  public static class SearchTerm {
    private String searchTerm;

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
  public static class QueryAggregation {
    private String fieldName;

    public QueryAggregation() {}

    public QueryAggregation(String fieldName) {
      this.fieldName = fieldName;
    }
  }
}
