package no.einnsyn.backend.common.statistics;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.search.SearchQueryBuilder;
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

  @Value("${application.elasticsearch.index}")
  private String elasticsearchIndex;

  public StatisticsService(ElasticsearchClient esClient) {
    this.esClient = esClient;
  }

  /**
   * @param statisticsParameters
   * @return
   */
  public StatisticsResponse getStatistics(StatisticsParameters statisticsParameters)
      throws EInnsynException {
    var queryBuilder = SearchQueryBuilder.getQueryBuilder(statisticsParameters);
    queryBuilder.filter(getVerifiedInnsynskravQuery());
    var query = queryBuilder.build();
    var aggregation = buildInnsynskravAggregation(statisticsParameters);

    var searchRequestBuilder = new SearchRequest.Builder();
    searchRequestBuilder.index(elasticsearchIndex);
    searchRequestBuilder.query(q -> q.bool(query));
    searchRequestBuilder.size(0);
    searchRequestBuilder.aggregations("innsynskrav", aggregation);
    // TODO: Add "downloads" when we collect this

    try {
      System.err.println(query.toString());
      System.err.println("----");
      System.err.println(aggregation.toString());
      log.debug("getStatistics Query: {}", query.toString());
      log.debug("getStatistics Aggregation: {}", aggregation.toString());
      var searchResponse = esClient.search(searchRequestBuilder.build(), Void.class);
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
      innsynskravStatistics.setCount((int) innsynskravAggregations.children().docCount());

      var responseBuckets = new ArrayList<StatisticsResponse.Innsynskrav.Bucket>();
      var buckets = innsynskravAggregations.children().aggregations().get("buckets");
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
   * Get the max / min created date for children of the given type
   *
   * @param statisticsParameters
   * @throws EInnsynException
   */
  MaxMinCreated getInnsynskravMaxMinCreated(StatisticsParameters statisticsParameters)
      throws EInnsynException {
    var queryBuilder = SearchQueryBuilder.getQueryBuilder(statisticsParameters);
    queryBuilder.filter(getVerifiedInnsynskravQuery());
    var query = queryBuilder.build();

    var aggregation =
        Aggregation.of(
            a ->
                a.children(c -> c.type("innsynskrav"))
                    .aggregations(
                        "min", Aggregation.of(inner -> inner.min(m -> m.field("created"))))
                    .aggregations(
                        "max", Aggregation.of(inner -> inner.max(m -> m.field("created")))));

    var searchRequestBuilder = new SearchRequest.Builder();
    searchRequestBuilder.index(elasticsearchIndex);
    searchRequestBuilder.query(q -> q.bool(query));
    searchRequestBuilder.size(0);
    searchRequestBuilder.trackTotalHits(TrackHits.of(b -> b.enabled(false)));
    searchRequestBuilder.aggregations("innsynskrav", aggregation);
    // TODO: Add "downloads" when we collect this
    var searchRequest = searchRequestBuilder.build();

    try {
      var maxMinResponse = esClient.search(searchRequest, Void.class);
      var innsynskravMinMax =
          maxMinResponse.aggregations().get("innsynskrav").children().aggregations();

      var minCreated = innsynskravMinMax.get("min").min().valueAsString();
      var maxCreated = innsynskravMinMax.get("max").max().valueAsString();

      return new MaxMinCreated(minCreated, maxCreated);
    } catch (IOException e) {
      throw new EInnsynException("Failed to get created interval", e);
    }
  }

  /**
   * Get a BoolQuery that filters for documents that has children of type "innsynskrav" where
   * "verified" = true
   */
  Query getVerifiedInnsynskravQuery() {
    return Query.of(
        f ->
            f.hasChild(
                hc ->
                    hc.type("innsynskrav")
                        .query(
                            q ->
                                q.bool(
                                    b ->
                                        b.must(
                                            m ->
                                                m.term(
                                                    t ->
                                                        t.field("verified")
                                                            .value(v -> v.booleanValue(true))))))));
  }

  /**
   * @param statisticsParameters
   * @return
   */
  Aggregation buildInnsynskravAggregation(StatisticsParameters statisticsParameters)
      throws EInnsynException {

    // If no "from" or "to" date is given, use the max / min created date of children
    var aggregateFrom = statisticsParameters.getAggregateFrom();
    var aggregateTo = statisticsParameters.getAggregateTo();
    if (!StringUtils.hasText(aggregateFrom) || !StringUtils.hasText(aggregateTo)) {
      var maxMinCreated = getInnsynskravMaxMinCreated(statisticsParameters);
      if (aggregateFrom == null) {
        aggregateFrom = maxMinCreated.getMinCreated();
      }
      if (aggregateTo == null) {
        aggregateTo = maxMinCreated.getMaxCreated();
      }
    }

    // Find calendarInterval based on from - to date
    CalendarInterval calendarInterval;

    // If there is still no from/to, we have no matches and default to day
    if (!StringUtils.hasText(aggregateFrom) || !StringUtils.hasText(aggregateTo)) {
      calendarInterval = CalendarInterval.Day;
    } else {
      var aggregateFromDate = ZonedDateTime.parse(aggregateFrom);
      var aggregateToDate = ZonedDateTime.parse(aggregateTo);

      // If there are more than 100 weeks between the dates, use months
      if (aggregateFromDate.plusWeeks(100).isBefore(aggregateToDate)) {
        calendarInterval = CalendarInterval.Month;
      }
      // If there are more than 100 days between the dates, use weeks
      else if (aggregateFromDate.plusDays(100).isBefore(aggregateToDate)) {
        calendarInterval = CalendarInterval.Week;
      }
      // If there are more than 100 hours between the dates, use days
      else if (aggregateFromDate.plusHours(100).isBefore(aggregateToDate)) {
        calendarInterval = CalendarInterval.Day;
      }
      // Anything less than 100 hours, use hours
      else {
        calendarInterval = CalendarInterval.Hour;
      }
    }

    var histogramAgg =
        Aggregation.of(
            a ->
                a.dateHistogram(
                    h -> h.field("created").calendarInterval(calendarInterval).minDocCount((1))));

    var aggregation =
        Aggregation.of(
            a -> a.children(c -> c.type("innsynskrav")).aggregations("buckets", histogramAgg));

    return aggregation;
  }

  @Getter
  @Setter
  private class MaxMinCreated {
    public String minCreated;
    public String maxCreated;

    public MaxMinCreated(String minCreated, String maxCreated) {
      this.minCreated = minCreated;
      this.maxCreated = maxCreated;
    }
  }
}
