package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@JsonInclude(Include.ALWAYS)
@Builder
public record DataExtraction(
    @JsonProperty(required = true) List<AttributeGroup> attributeGroups
) {
}
