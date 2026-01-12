package no.einnsyn.backend.common.statistics;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ObjIntConsumer;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.exceptions.models.InternalServerErrorException;
import no.einnsyn.backend.common.search.SearchQueryService;
import no.einnsyn.backend.common.statistics.models.StatisticsParameters;
import no.einnsyn.backend.common.statistics.models.StatisticsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@SuppressWarnings("java:S1192") // Allow string literals
public class StatisticsService {

  private static final int MAX_BUCKETS = 1000;
  private static final String INTERVAL_HOUR = "hour";
  private static final String INTERVAL_DAY = "day";
  private static final String INTERVAL_WEEK = "week";

  private final ElasticsearchClient esClient;
  private final SearchQueryService searchQueryService;

  @Value("${application.elasticsearch.index}")
  private String elasticsearchIndex;

  public StatisticsService(ElasticsearchClient esClient, SearchQueryService searchQueryService) {
    this.esClient = esClient;
    this.searchQueryService = searchQueryService;
  }

  /**
   * Query statistics based on the provided parameters
   *
   * @param statisticsParameters the parameters for filtering and aggregating statistics
   * @return the statistics response containing summary, time series, and metadata
   * @throws EInnsynException if the query fails
   */
  public StatisticsResponse query(StatisticsParameters statisticsParameters)
      throws EInnsynException {
    var queryBuilder = searchQueryService.getQueryBuilder(statisticsParameters);

    // Get aggregate from/to range. Incoming values has already been validated when parsed.
    var aggregateTo =
        statisticsParameters.getAggregateTo() != null
            ? LocalDate.parse(statisticsParameters.getAggregateTo()).plusDays(1).atStartOfDay()
            : LocalDate.now().plusDays(1).atStartOfDay();
    var aggregateFrom =
        statisticsParameters.getAggregateFrom() != null
            ? LocalDate.parse(statisticsParameters.getAggregateFrom()).atStartOfDay()
            : aggregateTo.minusYears(1);
    var calendarInterval =
        calculateCalendarInterval(
            statisticsParameters.getAggregateFrom(),
            statisticsParameters.getAggregateTo(),
            statisticsParameters.getAggregateInterval());

    // No need to check documents created after the aggregation range
    if (aggregateTo.isBefore(LocalDateTime.now())) {
      var createdDateRangeQuery = getCreatedDateRangeQuery(null, aggregateTo);
      if (createdDateRangeQuery != null) {
        queryBuilder.filter(f -> f.range(createdDateRangeQuery));
      }
    }

    var query = queryBuilder.build();
    var createdCountAggregation =
        buildCreatedCountAggregation(aggregateFrom, aggregateTo, calendarInterval);
    var innsynskravCountAggregation =
        buildInnsynskravCountAggregation(aggregateFrom, aggregateTo, calendarInterval);
    var fulltextCountAggregation =
        buildFulltextCountAggregation(aggregateFrom, aggregateTo, calendarInterval);

    var searchRequestBuilder = new SearchRequest.Builder();
    searchRequestBuilder.index(elasticsearchIndex);
    searchRequestBuilder.query(q -> q.bool(query));
    searchRequestBuilder.size(0); // Don't fetch results
    searchRequestBuilder.aggregations("innsynskravCount", innsynskravCountAggregation);
    searchRequestBuilder.aggregations("fulltextCount", fulltextCountAggregation);
    searchRequestBuilder.aggregations("createdCount", createdCountAggregation);
    // TODO: Add "downloads" when we collect this
    var searchRequest = searchRequestBuilder.build();

    try {
      log.debug("getStatistics() request: {}", searchRequest.toString());
      var searchResponse = esClient.search(searchRequest, Void.class);
      log.debug("getStatistics() response: {}", searchResponse.toString());
      return buildResponse(searchResponse, aggregateFrom, aggregateTo, calendarInterval);
    } catch (IOException e) {
      throw new InternalServerErrorException("Failed to get statistics", e);
    }
  }

  /**
   * Build statistics response from Elasticsearch search response
   *
   * @param response the Elasticsearch search response containing aggregations
   * @param statisticsParameters the parameters used for the query
   * @return the statistics response with populated summary, time series, and metadata
   */
  @SuppressWarnings("java:S1192") // Allow string literals
  StatisticsResponse buildResponse(
      SearchResponse<Void> response,
      LocalDateTime aggregateFrom,
      LocalDateTime aggregateTo,
      CalendarInterval calendarInterval) {
    var innsynskravAggregations = response.aggregations().get("innsynskravCount");
    var fulltextCountAggregations = response.aggregations().get("fulltextCount");
    var createdCountAggregations = response.aggregations().get("createdCount");
    var statisticsResponse = new StatisticsResponse();

    // Build summary
    var summary = new StatisticsResponse.Summary();
    statisticsResponse.setSummary(summary);

    // Set total document count
    if (createdCountAggregations != null && createdCountAggregations.isFilter()) {
      summary.setCreatedCount((int) createdCountAggregations.filter().docCount());
    } else {
      summary.setCreatedCount(0);
    }

    // Set total fulltext count
    if (fulltextCountAggregations != null && fulltextCountAggregations.isFilter()) {
      summary.setCreatedWithFulltextCount((int) fulltextCountAggregations.filter().docCount());
    } else {
      summary.setCreatedWithFulltextCount(0);
    }

    // Set total innsynskrav children count
    if (innsynskravAggregations != null && innsynskravAggregations.isChildren()) {
      var filteredAgg = innsynskravAggregations.children().aggregations().get("filtered");
      if (filteredAgg != null && filteredAgg.isFilter()) {
        summary.setCreatedInnsynskravCount((int) filteredAgg.filter().docCount());
      }
    }
    if (summary.getCreatedInnsynskravCount() == null) {
      summary.setCreatedInnsynskravCount(0);
    }

    // Build timeSeries - collect all unique time buckets from all aggregations
    var timeSeriesMap = new LinkedHashMap<String, StatisticsResponse.TimeSeries>();

    // Collect buckets from all aggregations
    collectTimeSeriesBuckets(
        createdCountAggregations, timeSeriesMap, StatisticsResponse.TimeSeries::setCreatedCount);
    collectTimeSeriesBuckets(
        fulltextCountAggregations,
        timeSeriesMap,
        StatisticsResponse.TimeSeries::setCreatedWithFulltextCount);

    // Handle innsynskrav aggregation (needs to extract filtered child aggregation first)
    if (innsynskravAggregations != null && innsynskravAggregations.isChildren()) {
      var filteredAgg = innsynskravAggregations.children().aggregations().get("filtered");
      collectTimeSeriesBuckets(
          filteredAgg, timeSeriesMap, StatisticsResponse.TimeSeries::setCreatedInnsynskravCount);
    }

    // Convert map to list maintaining insertion order
    var timeSeries = new ArrayList<>(timeSeriesMap.values());
    statisticsResponse.setTimeSeries(timeSeries);

    // Build metadata
    var metadata = new StatisticsResponse.Metadata();
    statisticsResponse.setMetadata(metadata);
    metadata.setAggregateFrom(aggregateFrom.toLocalDate().toString());
    // Internally we treat aggregateTo as an exclusive upper bound (start of the day after the
    // requested end date). The API response metadata should reflect the user-facing end date.
    metadata.setAggregateTo(aggregateTo.minusDays(1).toLocalDate().toString());
    metadata.setAggregateInterval(calendarInterval.jsonValue());

    return statisticsResponse;
  }

  /**
   * Build aggregation for counting verified innsynskrav children over time
   *
   * @param statisticsParameters the parameters for the aggregation
   * @return the children aggregation with date histogram buckets
   */
  Aggregation buildInnsynskravCountAggregation(
      LocalDateTime aggregateFrom, LocalDateTime aggregateTo, CalendarInterval calendarInterval) {

    // Filter children by verified, and created range
    var childrenFilterQueryBuilder = new BoolQuery.Builder();
    var aggregationDateRangeQuery = getCreatedDateRangeQuery(aggregateFrom, aggregateTo);
    if (aggregationDateRangeQuery != null) {
      childrenFilterQueryBuilder.filter(f -> f.range(aggregationDateRangeQuery));
    }

    // Filter by verified
    var verifiedTermQuery = TermQuery.of(t -> t.field("verified").value(v -> v.booleanValue(true)));
    childrenFilterQueryBuilder.filter(f -> f.term(verifiedTermQuery));

    var histogramAgg =
        Aggregation.of(
            a ->
                a.dateHistogram(
                    h -> h.field("created").calendarInterval(calendarInterval).minDocCount((1))));

    return Aggregation.of(
        a ->
            a.children(c -> c.type("innsynskrav"))
                .aggregations(
                    "filtered",
                    Aggregation.of(
                        f ->
                            f.filter(q -> q.bool(childrenFilterQueryBuilder.build()))
                                .aggregations("buckets", histogramAgg))));
  }

  /**
   * Build aggregation for counting all documents over time
   *
   * @param statisticsParameters the parameters for the aggregation
   * @return the filter aggregation with date histogram buckets for all documents
   */
  Aggregation buildCreatedCountAggregation(
      LocalDateTime aggregateFrom, LocalDateTime aggregateTo, CalendarInterval calendarInterval) {

    // Filter by created date range
    var aggregationDateRangeQuery = getCreatedDateRangeQuery(aggregateFrom, aggregateTo);
    var filterQueryBuilder = new BoolQuery.Builder();
    filterQueryBuilder.filter(f -> f.range(aggregationDateRangeQuery));

    var histogramAgg =
        Aggregation.of(
            a ->
                a.dateHistogram(
                    h -> h.field("created").calendarInterval(calendarInterval).minDocCount((1))));

    return Aggregation.of(
        f ->
            f.filter(q -> q.bool(filterQueryBuilder.build()))
                .aggregations("buckets", histogramAgg));
  }

  /**
   * Build aggregation for documents with fulltext property
   *
   * @param statisticsParameters the parameters for the aggregation
   * @param fulltextValue true or false to filter by fulltext property
   * @return the filter aggregation with date histogram buckets for documents with specified
   *     fulltext value
   */
  Aggregation buildFulltextCountAggregation(
      LocalDateTime aggregateFrom, LocalDateTime aggregateTo, CalendarInterval calendarInterval) {

    // Filter by created date range
    var aggregationDateRangeQuery = getCreatedDateRangeQuery(aggregateFrom, aggregateTo);
    var filterQueryBuilder = new BoolQuery.Builder();
    filterQueryBuilder.filter(f -> f.range(aggregationDateRangeQuery));

    // Filter by fulltext = true
    var fulltextTermQuery = TermQuery.of(t -> t.field("fulltext").value(v -> v.booleanValue(true)));
    filterQueryBuilder.filter(f -> f.term(fulltextTermQuery));

    var histogramAgg =
        Aggregation.of(
            a ->
                a.dateHistogram(
                    h -> h.field("created").calendarInterval(calendarInterval).minDocCount((1))));

    return Aggregation.of(
        f ->
            f.filter(q -> q.bool(filterQueryBuilder.build()))
                .aggregations("buckets", histogramAgg));
  }

  /**
   * Select a date histogram bucket interval for the given date range.
   *
   * <p>The returned interval is the most fine-grained interval that does not exceed {@link
   * #MAX_BUCKETS}. If {@code requestedInterval} is provided, it is treated as the <em>maximum</em>
   * desired resolution (hour/day/week/month). If that would exceed the bucket limit, the method
   * falls back to progressively coarser intervals until it fits.
   *
   * <p>If {@code requestedInterval} is {@code null} / blank / unrecognized, the method defaults to
   * trying {@code hour} first.
   *
   * @param aggregateFrom the start date in ISO-8601 format (yyyy-MM-dd)
   * @param aggregateTo the end date in ISO-8601 format (yyyy-MM-dd)
   * @param requestedInterval the desired maximum resolution: hour/day/week/month (case-insensitive)
   * @return the chosen bucket interval (Hour/Day/Week/Month/Year)
   */
  private CalendarInterval calculateCalendarInterval(
      String aggregateFrom, String aggregateTo, String requestedInterval) {

    var aggregateFromDate = LocalDate.parse(aggregateFrom).atStartOfDay();
    var aggregateToDate = LocalDate.parse(aggregateTo).atStartOfDay();

    if (!StringUtils.hasText(requestedInterval)) {
      requestedInterval = INTERVAL_HOUR;
    } else {
      requestedInterval = requestedInterval.toLowerCase();
    }

    if (INTERVAL_HOUR.equals(requestedInterval)) {
      var hours = ChronoUnit.HOURS.between(aggregateFromDate, aggregateToDate) + 24;
      if (hours <= MAX_BUCKETS) {
        return CalendarInterval.Hour;
      }
      // Resolution too high, fall back to day
      requestedInterval = INTERVAL_DAY;
    }

    if (INTERVAL_DAY.equals(requestedInterval)) {
      var days = ChronoUnit.DAYS.between(aggregateFromDate, aggregateToDate) + 1;
      if (days <= MAX_BUCKETS) {
        return CalendarInterval.Day;
      }
      // Resolution too high, fall back to week
      requestedInterval = INTERVAL_WEEK;
    }

    if (INTERVAL_WEEK.equals(requestedInterval)) {
      var weeks = ChronoUnit.WEEKS.between(aggregateFromDate, aggregateToDate) + 1;
      if (weeks <= MAX_BUCKETS) {
        return CalendarInterval.Week;
      }
    }

    return CalendarInterval.Month;
  }

  /**
   * Create a range query for filtering documents by created date
   *
   * @param aggregateFrom the start date (inclusive), or null for no lower bound
   * @param aggregateTo the end date (inclusive), or null for no upper bound
   * @return the range query, or null if both parameters are empty
   */
  RangeQuery getCreatedDateRangeQuery(LocalDateTime aggregateFrom, LocalDateTime aggregateTo) {
    if (aggregateFrom == null && aggregateTo == null) {
      return null;
    }

    return RangeQuery.of(
        r ->
            r.date(
                d -> {
                  if (aggregateFrom != null) {
                    d.field("created").gte(aggregateFrom.toString());
                  }
                  if (aggregateTo != null) {
                    d.field("created").lte(aggregateTo.toString());
                  }
                  return d;
                }));
  }

  /**
   * Helper method to collect time series buckets from aggregation results into a map
   *
   * @param aggregation the aggregation result to extract buckets from
   * @param timeSeriesMap the map to populate with time series data points
   * @param setter the consumer to set the count on each data point
   */
  private void collectTimeSeriesBuckets(
      Aggregate aggregation,
      Map<String, StatisticsResponse.TimeSeries> timeSeriesMap,
      ObjIntConsumer<StatisticsResponse.TimeSeries> setter) {
    if (aggregation != null && aggregation.isFilter()) {
      var buckets = aggregation.filter().aggregations().get("buckets");
      if (buckets != null && buckets.isDateHistogram()) {
        var dateHistogram = buckets.dateHistogram();
        for (var bucket : dateHistogram.buckets().array()) {
          var time = bucket.keyAsString();
          var dataPoint =
              timeSeriesMap.computeIfAbsent(
                  time,
                  k -> {
                    var point = new StatisticsResponse.TimeSeries();
                    point.setTime(k);
                    point.setCreatedCount(0);
                    point.setCreatedWithFulltextCount(0);
                    point.setCreatedInnsynskravCount(0);
                    return point;
                  });
          setter.accept(dataPoint, (int) bucket.docCount());
        }
      }
    }
  }
}
