package no.einnsyn.backend.common.robots;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves a robots.txt that disallows all crawling. Static resource mappings are disabled
 * (spring.web.resources.add-mappings: false), so this file has to be served by a controller.
 */
@RestController
public class RobotsController {

  private static final String ROBOTS_TXT = "User-agent: *\nDisallow: /\n";

  @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
  public String robotsTxt() {
    return ROBOTS_TXT;
  }
}
