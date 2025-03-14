package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@JsonInclude(Include.ALWAYS)
@Builder
public record DataExtraction(
    @JsonProperty long id,
    @JsonProperty StructuredQuery content,
    @JsonProperty String label,
    @JsonProperty String comment,
    @JsonProperty String lastModified,
    @JsonProperty String createdBy,
    @JsonProperty Boolean isValid
) {

}
