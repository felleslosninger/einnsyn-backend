package no.einnsyn.backend.validation.isodatetime;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.format.DateTimeFormatter;

@Constraint(validatedBy = IsoDateTimeValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface IsoDateTime {
  String message() default "Invalid date / time format.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  Format format() default Format.ISO_DATE;

  boolean allowRelative() default false;

  enum Format {
    ISO_DATE(DateTimeFormatter.ISO_DATE),
    ISO_DATE_TIME(DateTimeFormatter.ISO_DATE_TIME),
    ISO_DATE_OR_DATE_TIME(null);

    private final DateTimeFormatter formatter;

    Format(DateTimeFormatter formatter) {
      this.formatter = formatter;
    }

    public DateTimeFormatter getFormatter() {
      return formatter;
    }
  }
}
