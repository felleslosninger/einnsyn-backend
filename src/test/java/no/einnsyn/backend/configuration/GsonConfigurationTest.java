package no.einnsyn.backend.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GsonConfigurationTest extends EinnsynControllerTestBase {

  @Test
  void testPasswordRemovalFromLogs() throws Exception {

    // Capture logs
    var logger = (Logger) LoggerFactory.getLogger(BaseService.class);
    var listAppender = new ListAppender<ILoggingEvent>();
    listAppender.start();
    logger.addAppender(listAppender);

    // Add Bruker
    var brukerJSON = getBrukerJSON();
    var brukerPassword = brukerJSON.getString("password");
    var response = post("/bruker", brukerJSON);
    var responseDTO = gson.fromJson(response.getBody(), BrukerDTO.class);

    for (var event : listAppender.list) {
      for (var argument : event.getArgumentArray()) {
        var payload = argument.toString();
        if (payload.startsWith("payload") && payload.contains(brukerPassword)) {
          throw new Exception("Password found in logs");
        }
      }
    }

    // Delete
    assertEquals(HttpStatus.OK, deleteAdmin("/bruker/" + responseDTO.getId()).getStatusCode());
  }

  @Test
  void testDenyUnknownPropertiesInBody() throws Exception {
    var brukerJSON = getBrukerJSON();
    brukerJSON.put("unknownProperty", "value");
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().contains("Unknown property"));
    assertTrue(response.getBody().contains("unknownProperty"));
  }

  @Test
  void testPropertyOrder() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Make sure "id" is before "tittel" in the response
    assertTrue(response.getBody().indexOf("id") < response.getBody().indexOf("tittel"));

    // Delete
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }
}
