package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@JsonInclude(Include.NON_EMPTY)
@Builder
public record Dataquery(
    @JsonProperty long id,
    @JsonProperty Crtdl content,
    @JsonProperty String label,
    @JsonProperty String comment,
    @JsonProperty String lastModified,
    @JsonProperty String createdBy,
    @JsonProperty CrtdlSectionInfo ccdl,
    @JsonProperty CrtdlSectionInfo dataExtraction,
    @JsonProperty Long resultSize
) {

}
