package no.einnsyn.backend.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.List;
import no.einnsyn.backend.EinnsynTestBase;
import no.einnsyn.clients.ip.IPSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(properties = {"management.defaults.metrics.export.enabled=true"})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MetricsConfigTest extends EinnsynTestBase {

  @Autowired private MeterRegistry meterRegistry;
  // Mocked to satisfy nested dependency in InnsynskravBestillingTestController
  @MockitoBean protected IPSender ipSender;

  /** Verify that the URI normalization is working. IDs should be replaced with "{id}". */
  @Test
  void testUriNormalization() {
    meterRegistry
        .counter("http.client.requests", "uri", "/saksmappe/sm_01j2jvd6ssehptmqn4yzv7c330")
        .increment();
    assertNotNull(
        meterRegistry.get("http.client.requests").tags(List.of(Tag.of("uri", "/saksmappe/{id}"))));

    meterRegistry
        .counter("http.client.requests", "uri", "/saksmappe/sm_01j2jvd6ssehptmqn4yzv7c330/foo")
        .increment();
    assertNotNull(
        meterRegistry
            .get("http.client.requests")
            .tags(List.of(Tag.of("uri", "/saksmappe/{id}/foo"))));

    meterRegistry.counter("http.client.requests", "uri", "/bruker/test@example.com").increment();
    assertNotNull(
        meterRegistry.get("http.client.requests").tags(List.of(Tag.of("uri", "/bruker/{email}"))));

    meterRegistry
        .counter("http.client.requests", "uri", "/journalpost/123e4567-e89b-12d3-a456-426614174000")
        .increment();
    assertNotNull(
        meterRegistry
            .get("http.client.requests")
            .tags(List.of(Tag.of("uri", "/journalpost/{uuid}"))));
  }
}
