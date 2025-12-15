package no.einnsyn.backend.tasks.handlers.lagretsoek;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekRepository;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekService;
import no.einnsyn.backend.entities.lagretsoek.LegacyQueryConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class LegacyLagretSoekConversionScheduler {

  private final LagretSoekRepository lagretSoekRepository;
  private final LagretSoekService lagretSoekService;
  private final LegacyQueryConverter legacyQueryConverter;
  private final ElasticsearchClient esClient;
  private final ObjectMapper objectMapper;

  @Value("${application.legacyLagretSoekConversion.dryRun:true}")
  private boolean dryRun;

  @Value("${application.legacyLagretSoekConversion.batchSize:500}")
  private int batchSize;

  public LegacyLagretSoekConversionScheduler(
      LagretSoekRepository lagretSoekRepository,
      LagretSoekService lagretSoekService,
      LegacyQueryConverter legacyQueryConverter,
      ElasticsearchClient esClient,
      ObjectMapper objectMapper) {
    this.lagretSoekRepository = lagretSoekRepository;
    this.lagretSoekService = lagretSoekService;
    this.legacyQueryConverter = legacyQueryConverter;
    this.esClient = esClient;
    this.objectMapper = objectMapper;
  }

  @Scheduled(
      fixedDelayString = "${application.legacyLagretSoekConversion.interval:PT1H}",
      initialDelayString = "${application.legacyLagretSoekConversion.initialDelay:PT1M}")
  @SchedulerLock(
      name = "LegacyLagretSoekConversionScheduler",
      lockAtLeastFor = "PT1M",
      lockAtMostFor = "PT10M")
  @Transactional
  public void convertLegacyLagretSoek() {
    var pageRequest = PageRequest.of(0, batchSize);
    var slice =
        lagretSoekRepository.findByLegacyQueryIsNotNullAndSearchParametersIsNull(pageRequest);

    if (slice.isEmpty()) {
      return;
    }

    log.info("Found {} legacy LagretSoek to convert", slice.getNumberOfElements());

    for (var lagretSoek : slice) {
      try {
        var legacyQuery = lagretSoek.getLegacyQuery();
        var searchParameters = legacyQueryConverter.convertLegacyQuery(legacyQuery);
        var searchParametersString = objectMapper.writeValueAsString(searchParameters);

        if (dryRun) {
          log.info(
              "Dry-run: Would convert legacy query '{}' to '{}' for LagretSoek {}",
              legacyQuery,
              searchParametersString,
              lagretSoek.getId());
          log.info(
              "Dry-run: Would delete legacy Elasticsearch document with id {}",
              lagretSoek.getLegacyId());
        } else {
          lagretSoek.setSearchParameters(searchParametersString);
          lagretSoekRepository.save(lagretSoek);

          if (lagretSoek.getLegacyId() != null) {
            esClient.delete(
                d ->
                    d.index(lagretSoekService.getElasticsearchIndex())
                        .id(lagretSoek.getLegacyId().toString()));
          }
          log.debug("Converted LagretSoek {}", lagretSoek.getId());
        }

      } catch (Exception e) {
        log.error("Failed to convert LagretSoek {}", lagretSoek.getId(), e);
      }
    }
  }
}
