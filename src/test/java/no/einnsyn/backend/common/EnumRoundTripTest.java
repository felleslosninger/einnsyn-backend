package no.einnsyn.backend.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * Round-trips every enum in the code base that follows the API-spec-generated shape (a toJson()
 * instance method and a static fromValue(String) factory), so all generated enums stay covered
 * without a hand-written test per enum.
 */
class EnumRoundTripTest {

  private List<Class<?>> findJsonEnums() throws Exception {
    var provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AssignableTypeFilter(Enum.class));

    var jsonEnums = new ArrayList<Class<?>>();
    for (var beanDefinition : provider.findCandidateComponents("no.einnsyn.backend")) {
      var clazz = Class.forName(beanDefinition.getBeanClassName());
      if (!clazz.isEnum()) {
        continue;
      }
      try {
        clazz.getMethod("toJson");
        clazz.getMethod("fromValue", String.class);
      } catch (NoSuchMethodException e) {
        continue;
      }
      jsonEnums.add(clazz);
    }
    return jsonEnums;
  }

  @Test
  void testEnumJsonRoundTrip() throws Exception {
    var jsonEnums = findJsonEnums();

    // Sanity check that the classpath scan still finds the generated enums
    assertTrue(
        jsonEnums.size() >= 15,
        "Expected at least 15 enums with toJson/fromValue, found " + jsonEnums.size());

    for (var clazz : jsonEnums) {
      Method toJson = clazz.getMethod("toJson");
      Method fromValue = clazz.getMethod("fromValue", String.class);

      // Every constant can be recovered from its own JSON value
      for (var constant : clazz.getEnumConstants()) {
        var jsonValue = (String) toJson.invoke(constant);
        assertNotNull(jsonValue, clazz.getName() + "." + constant + " has a null JSON value");
        assertEquals(
            constant,
            fromValue.invoke(null, jsonValue),
            clazz.getName() + ".fromValue does not round-trip \"" + jsonValue + "\"");
        assertNotNull(constant.toString());
      }

      // Unknown values are rejected
      var exception =
          assertThrows(
              InvocationTargetException.class,
              () -> fromValue.invoke(null, "no-such-enum-value"),
              clazz.getName() + ".fromValue accepted an unknown value");
      assertInstanceOf(
          IllegalArgumentException.class,
          exception.getCause(),
          clazz.getName() + ".fromValue threw an unexpected exception type");
    }
  }
}
