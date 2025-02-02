package de.numcodex.feasibility_gui_backend.dse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record Field(
    @JsonProperty String id,
    @JsonProperty DisplayEntry display,
    @JsonProperty DisplayEntry description,
    @JsonProperty List<String> referencedProfiles,
    @JsonProperty String type,
    @JsonProperty boolean recommended,
    @JsonProperty boolean required,
    @JsonProperty List<Field> children
) {
}
