package de.numcodex.feasibility_gui_backend.query.api.validation;

import de.numcodex.feasibility_gui_backend.query.api.QueryTemplate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link QueryTemplate} that always passes no matter what instance gets checked.
 */
public class QueryTemplatePassValidator implements ConstraintValidator<QueryTemplateValidation, QueryTemplate> {
    @Override
    public boolean isValid(QueryTemplate queryTemplate, ConstraintValidatorContext constraintValidatorContext) {
        return true;
    }
}
