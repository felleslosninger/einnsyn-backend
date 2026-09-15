package no.einnsyn.backend.common.robots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.einnsyn.backend.EinnsynControllerTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RobotsControllerTest extends EinnsynControllerTestBase {

  @Test
  void testRobotsTxtIsServedToAnonymousClients() throws Exception {
    var response = getAnon("/robots.txt");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getHeaders().getContentType());
    assertTrue(MediaType.TEXT_PLAIN.isCompatibleWith(response.getHeaders().getContentType()));
    assertEquals("User-agent: *\nDisallow: /\n", response.getBody());
  }
}
