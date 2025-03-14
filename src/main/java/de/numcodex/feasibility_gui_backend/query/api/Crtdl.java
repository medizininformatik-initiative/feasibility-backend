package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record Crtdl(
    @JsonProperty String version,
    @JsonProperty String display,
    @JsonProperty StructuredQuery cohortDefinition,
    @JsonProperty DataExtraction dataExtraction
) {
}
