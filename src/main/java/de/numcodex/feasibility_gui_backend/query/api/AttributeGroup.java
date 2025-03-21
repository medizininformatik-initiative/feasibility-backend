package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.net.URI;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record AttributeGroup(
    @JsonProperty String id,
    @JsonProperty String name,
    @JsonProperty URI groupReference,
    @JsonProperty Boolean includeReferenceOnly,
    @JsonProperty List<Attribute> attributes,
    @JsonProperty List<Filter> filter
) {
}
