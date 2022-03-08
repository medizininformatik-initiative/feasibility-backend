package de.numcodex.feasibility_gui_backend.query.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = QueryValidator.class)
public @interface JsonSchemaValidation {
  String message() default "JSON validation failed";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
