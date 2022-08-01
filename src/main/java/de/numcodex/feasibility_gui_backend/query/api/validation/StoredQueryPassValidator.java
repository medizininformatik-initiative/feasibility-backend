package de.numcodex.feasibility_gui_backend.query.api.validation;

import de.numcodex.feasibility_gui_backend.query.api.StoredQuery;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator for {@link StoredQuery} that always passes no matter what instance gets checked.
 */
public class StoredQueryPassValidator implements ConstraintValidator<StoredQueryValidation, StoredQuery> {
    @Override
    public boolean isValid(StoredQuery storedQuery, ConstraintValidatorContext constraintValidatorContext) {
        return true;
    }
}
