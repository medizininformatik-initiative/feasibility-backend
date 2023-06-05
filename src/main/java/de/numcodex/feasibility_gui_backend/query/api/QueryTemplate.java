package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.validation.QueryTemplateValidation;
import java.util.List;

@JsonInclude(Include.NON_NULL)
@QueryTemplateValidation
public record QueryTemplate(
    @JsonProperty long id,
    @JsonProperty StructuredQuery content,
    @JsonProperty String label,
    @JsonProperty String comment,
    @JsonProperty String lastModified,
    @JsonProperty String createdBy,
    @JsonProperty List<TermCode> invalidTerms,
    @JsonProperty Boolean isValid
) {

}
