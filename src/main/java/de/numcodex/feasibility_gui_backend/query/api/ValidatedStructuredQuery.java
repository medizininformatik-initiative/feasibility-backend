package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import lombok.Builder;

import java.util.List;

@Builder
public record ValidatedStructuredQuery(
    @JsonProperty StructuredQuery query,
    @JsonProperty List<Criterion> invalidCriteria
) {

}
