package no.einnsyn.apiv3.utils.Detector;

import org.springframework.stereotype.Component;

@Component
public class Detector {

  public Detector() {}

  public boolean hasSSN(String s) {
    return s.matches(".*\\d{11}.*");
  }
}
