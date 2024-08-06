package de.numcodex.feasibility_gui_backend.dse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record Filter(
    @JsonProperty String type,
    @JsonProperty String name,
    @JsonProperty(value = "ui_type") String uiType,
    @JsonProperty String referencedCriteriaSet
) {
}
