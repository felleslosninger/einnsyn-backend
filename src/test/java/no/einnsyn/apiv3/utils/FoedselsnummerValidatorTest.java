package no.einnsyn.apiv3.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class FoedselsnummerValidatorTest {
  // @formatter:off
  List<String> validFoedselsnummers = Arrays.asList(
    "05063826601",
    "31085314494",
    "25038738758",
    "03042535418",
    "04041870708",
    "08061939521",
    "28077848004",
    "14051802811",
    "02120818212",
    "10046038385"
  );
  // @formatter:on

  // @formatter:off
  List<String> invalidFoedselsnummers = Arrays.asList(
    "050638266010", // Extra integer
    "3108531a449", // Contains non-integer character
    "32038738758", // 32 days in month
    "03042535428", // Wrong k1
    "04041870709", // Wrong k2
    "08131939521", // 13 months
    "2807784800", // Too few integers
    "15051802111", // Invalid checksum
    "02120818202", // Invalid checksum
    "10046038375" // Invalid checksum
  );
  // @formatter:on

  @Test
  void checkValidFoedselsnummers() {
    validFoedselsnummers.forEach(fnr -> {
      assertTrue(FoedselsnummerValidator.isValid(fnr), fnr + " should be valid");
    });
  }

  @Test
  void checkInvalidFoedselsnummers() {
    invalidFoedselsnummers.forEach(fnr -> {
      assertFalse(FoedselsnummerValidator.isValid(fnr), fnr + " should be invalid");
    });
  }
}
