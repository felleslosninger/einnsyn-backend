package no.einnsyn.backend.common.statistics;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.HasChildQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.search.SearchQueryService;
import no.einnsyn.backend.common.statistics.models.StatisticsParameters;
import no.einnsyn.backend.common.statistics.models.StatisticsResponse;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class StatisticsService {

  private final ElasticsearchClient esClient;
  private final SearchQueryService searchQueryService;

  @Value("${application.elasticsearch.index}")
  private String elasticsearchIndex;

  public StatisticsService(ElasticsearchClient esClient, SearchQueryService searchQueryService) {
    this.esClient = esClient;
    this.searchQueryService = searchQueryService;
  }

  /**
   * @param statisticsParameters
   * @return
   */
  public StatisticsResponse getStatistics(StatisticsParameters statisticsParameters)
      throws EInnsynException {
    var queryBuilder = searchQueryService.getQueryBuilder(statisticsParameters);

    // Filter by documents having verified innsynskrav)
    var hasInnsynskravChildrenQuery = getHasInnsynskravChildrenQuery();
    queryBuilder.filter(q -> q.hasChild(hasInnsynskravChildrenQuery));

    // No need to check documents created after the aggregation range
    var createdDateRangeQuery =
        getAggregationDateRangeQuery(null, statisticsParameters.getAggregateTo());
    if (createdDateRangeQuery != null) {
      queryBuilder.filter(f -> f.range(createdDateRangeQuery));
    }

    var query = queryBuilder.build();
    var aggregation = buildInnsynskravAggregation(statisticsParameters);

    var searchRequestBuilder = new SearchRequest.Builder();
    searchRequestBuilder.index(elasticsearchIndex);
    searchRequestBuilder.query(q -> q.bool(query));
    searchRequestBuilder.size(0);
    searchRequestBuilder.aggregations("innsynskrav", aggregation);
    // TODO: Add "downloads" when we collect this
    var searchRequest = searchRequestBuilder.build();

    try {
      log.debug("getStatistics() request: {}", searchRequest.toString());
      var searchResponse = esClient.search(searchRequest, Void.class);
      log.debug("getStatistics() response: {}", searchResponse.toString());
      var statisticsResponse = buildResponse(searchResponse);
      return statisticsResponse;
    } catch (IOException e) {
      throw new EInnsynException("Failed to get statistics", e);
    }
  }

  /**
   * @param response
   * @return
   */
  StatisticsResponse buildResponse(SearchResponse<Void> response) {
    var innsynskravAggregations = response.aggregations().get("innsynskrav");
    var statisticsResponse = new StatisticsResponse();

    if (innsynskravAggregations.isChildren()) {
      var innsynskravStatistics = statisticsResponse.new Innsynskrav();
      statisticsResponse.setInnsynskrav(innsynskravStatistics);

      var filteredAgg = innsynskravAggregations.children().aggregations().get("filtered");
      innsynskravStatistics.setCount((int) filteredAgg.filter().docCount());
      var responseBuckets = new ArrayList<StatisticsResponse.Innsynskrav.Bucket>();
      var buckets = filteredAgg.filter().aggregations().get("buckets");
      if (buckets != null && buckets.isDateHistogram()) {
        var dateHistogram = buckets.dateHistogram();
        for (var bucket : dateHistogram.buckets().array()) {
          var responseBucket = innsynskravStatistics.new Bucket();
          responseBucket.setTime(bucket.keyAsString());
          responseBucket.setCount((int) bucket.docCount());
          responseBuckets.add(responseBucket);
        }
      }
      innsynskravStatistics.setBucket(responseBuckets);
    }

    return statisticsResponse;
  }

  /**
   * @param statisticsParameters
   * @return
   */
  Aggregation buildInnsynskravAggregation(StatisticsParameters statisticsParameters)
      throws EInnsynException {

    // If no "from" or "to" date is given, use the max / min created date of children
    var aggregateTo = statisticsParameters.getAggregateTo();
    if (!StringUtils.hasText(aggregateTo)) {
      aggregateTo = LocalDate.now().toString();
    }
    var aggregateFrom = statisticsParameters.getAggregateFrom();
    if (!StringUtils.hasText(aggregateFrom)) {
      aggregateFrom = LocalDate.parse(aggregateTo).minusYears(1).toString();
    }

    // Find calendarInterval based on from - to date
    CalendarInterval calendarInterval;

    // If there is still no from/to, we have no matches and default to day
    if (!StringUtils.hasText(aggregateFrom) || !StringUtils.hasText(aggregateTo)) {
      calendarInterval = CalendarInterval.Day;
    } else {
      var aggregateFromDate = LocalDate.parse(aggregateFrom);
      var aggregateToDate = LocalDate.parse(aggregateTo);

      // If there are more than 100 weeks between the dates, use months
      if (aggregateFromDate.plusWeeks(100).isBefore(aggregateToDate)) {
        calendarInterval = CalendarInterval.Month;
      }
      // If there are more than 100 days between the dates, use weeks
      else if (aggregateFromDate.plusDays(100).isBefore(aggregateToDate)) {
        calendarInterval = CalendarInterval.Week;
      }
      // If there are more than 96 hours (4*24) between the dates, use days
      else if (aggregateFromDate.plusDays(4).isBefore(aggregateToDate)) {
        calendarInterval = CalendarInterval.Day;
      }
      // Anything less than 100 hours, use hours
      else {
        calendarInterval = CalendarInterval.Hour;
      }
    }

    // Filter children by verified, and created range
    var filterQueryBuilder = new BoolQuery.Builder();
    var aggregationDateRangeQuery = getAggregationDateRangeQuery(aggregateFrom, aggregateTo);
    if (aggregationDateRangeQuery != null) {
      filterQueryBuilder.filter(f -> f.range(aggregationDateRangeQuery));
    }

    // Filter by verified
    var verifiedTermQuery = getVerifiedTermQuery();
    filterQueryBuilder.filter(f -> f.term(verifiedTermQuery));

    var histogramAgg =
        Aggregation.of(
            a ->
                a.dateHistogram(
                    h -> h.field("created").calendarInterval(calendarInterval).minDocCount((1))));

    var aggregation =
        Aggregation.of(
            a ->
                a.children(c -> c.type("innsynskrav"))
                    .aggregations(
                        "filtered",
                        Aggregation.of(
                            f ->
                                f.filter(q -> q.bool(filterQueryBuilder.build()))
                                    .aggregations("buckets", histogramAgg))));

    return aggregation;
  }

  /**
   * @param statisticsParameters
   * @return
   */
  RangeQuery getAggregationDateRangeQuery(String aggregateFrom, String aggregateTo) {
    if (StringUtils.hasText(aggregateFrom) || StringUtils.hasText(aggregateTo)) {
      return RangeQuery.of(
          r ->
              r.date(
                  d -> {
                    if (StringUtils.hasText(aggregateFrom)) {
                      d.field("created").gte(aggregateFrom);
                    }
                    if (StringUtils.hasText(aggregateTo)) {
                      d.field("created").lte(aggregateTo);
                    }
                    return d;
                  }));
    }

    return null;
  }

  TermQuery getVerifiedTermQuery() {
    return TermQuery.of(t -> t.field("verified").value(v -> v.booleanValue(true)));
  }

  /**
   * Get a BoolQuery that filters for documents that has children of type "innsynskrav" where
   * "verified" = true
   */
  HasChildQuery getHasInnsynskravChildrenQuery() {
    return HasChildQuery.of(
        hc ->
            hc.type("innsynskrav")
                .query(q -> q.bool(b -> b.must(m -> m.term(getVerifiedTermQuery())))));
  }
}
