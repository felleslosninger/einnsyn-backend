package no.einnsyn.apiv3.utils;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class IRIMatcherTest {

  @Test
  void testMatches() {
    assert (IRIMatcher.matches("http://example.com"));
    assert (IRIMatcher.matches("https://example.com"));
    assert (IRIMatcher.matches("ftp://example.com"));
    assert (IRIMatcher.matches("http://example.com/foo"));
    assert (IRIMatcher.matches("http://example.com/foo/bar"));
    assert (IRIMatcher.matches("my-app://path/to/resource"));
    assert (IRIMatcher.matches("ssh+git://git@github.com:username/repository.git"));
  }

  @Test
  void testNonMatches() {
    assert (!IRIMatcher.matches("example.com"));
    assert (!IRIMatcher.matches("http:/example.com"));
    assert (!IRIMatcher.matches("jp_01jd98akjwfdfawftx8cb05g42"));
  }
}
