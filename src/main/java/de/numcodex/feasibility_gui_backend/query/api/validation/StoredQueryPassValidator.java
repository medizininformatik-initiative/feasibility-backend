package de.numcodex.feasibility_gui_backend.query.api.validation;

import de.numcodex.feasibility_gui_backend.query.api.QueryTemplate;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator for {@link QueryTemplate} that always passes no matter what instance gets checked.
 */
public class StoredQueryPassValidator implements ConstraintValidator<StoredQueryValidation, QueryTemplate> {
    @Override
    public boolean isValid(QueryTemplate queryTemplate, ConstraintValidatorContext constraintValidatorContext) {
        return true;
    }
}
