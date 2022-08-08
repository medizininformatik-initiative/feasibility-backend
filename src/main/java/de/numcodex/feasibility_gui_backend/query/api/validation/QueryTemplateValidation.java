package de.numcodex.feasibility_gui_backend.query.api.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = QueryTemplateValidator.class)
public @interface QueryTemplateValidation {
    String message() default "Stored query is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
