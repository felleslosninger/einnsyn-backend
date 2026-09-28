package no.einnsyn.backend.validation.unknownparameters;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import no.einnsyn.backend.common.exceptions.models.ValidationException;
import no.einnsyn.backend.common.exceptions.models.ValidationException.FieldError;
import no.einnsyn.backend.common.queryparameters.models.QueryParameters;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rejects requests carrying query parameters the handler does not declare. Spring's data binder
 * drops unknown parameters silently, which hides typos and lets clients bypass caches by varying
 * junk parameters on heavy endpoints.
 */
@Component
public class UnknownQueryParametersInterceptor implements HandlerInterceptor {

  private final Map<Method, Set<String>> allowedParametersByMethod = new ConcurrentHashMap<>();

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws ValidationException {
    // Only our own controllers; actuator and Spring's error controller are left alone.
    if (!(handler instanceof HandlerMethod handlerMethod)
        || !handlerMethod.getBeanType().getPackageName().startsWith("no.einnsyn.")) {
      return true;
    }

    var allowed =
        allowedParametersByMethod.computeIfAbsent(
            handlerMethod.getMethod(), UnknownQueryParametersInterceptor::allowedParameters);
    var fieldErrors = new ArrayList<FieldError>();
    for (var entry : request.getParameterMap().entrySet()) {
      if (!allowed.contains(entry.getKey())) {
        fieldErrors.add(
            new FieldError(
                entry.getKey(), String.join(", ", entry.getValue()), "Unknown query parameter"));
      }
    }
    if (fieldErrors.isEmpty()) {
      return true;
    }

    var names =
        fieldErrors.stream().map(FieldError::getFieldName).collect(Collectors.joining(", "));
    var label =
        fieldErrors.size() == 1 ? "Unknown query parameter: " : "Unknown query parameters: ";
    throw new ValidationException(label + names, fieldErrors);
  }

  /**
   * Names the handler accepts: its @RequestParam arguments and the fields of its QueryParameters
   * arguments, including inherited ones.
   */
  private static Set<String> allowedParameters(Method method) {
    var names = new HashSet<String>();
    for (var parameter : method.getParameters()) {
      var requestParam = parameter.getAnnotation(RequestParam.class);
      if (requestParam != null) {
        var name = requestParam.name().isEmpty() ? requestParam.value() : requestParam.name();
        names.add(name.isEmpty() ? parameter.getName() : name);
      } else if (QueryParameters.class.isAssignableFrom(parameter.getType())) {
        for (Class<?> clazz = parameter.getType();
            clazz != Object.class;
            clazz = clazz.getSuperclass()) {
          for (var field : clazz.getDeclaredFields()) {
            var modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
              names.add(field.getName());
            }
          }
        }
      }
    }
    return Set.copyOf(names);
  }
}
