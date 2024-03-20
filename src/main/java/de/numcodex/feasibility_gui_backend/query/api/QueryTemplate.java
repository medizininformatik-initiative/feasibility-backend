package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.query.api.validation.QueryTemplateValidation;
import lombok.Builder;

import java.util.List;

@JsonInclude(Include.NON_NULL)
@QueryTemplateValidation
@Builder
public record QueryTemplate(
    @JsonProperty long id,
    @JsonProperty StructuredQuery content,
    @JsonProperty String label,
    @JsonProperty String comment,
    @JsonProperty String lastModified,
    @JsonProperty String createdBy,
    @JsonProperty List<Criterion> invalidCriteria,
    @JsonProperty Boolean isValid
) {

}
