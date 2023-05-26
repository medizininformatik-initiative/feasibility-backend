package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.query.api.validation.StructuredQueryValidation;

import java.net.URI;
import java.util.List;

@JsonInclude(Include.NON_EMPTY)
@StructuredQueryValidation
public record StructuredQuery(
    @JsonProperty URI version,
    @JsonProperty("inclusionCriteria") List<List<Criterion>> inclusionCriteria,
    @JsonProperty("exclusionCriteria") List<List<Criterion>> exclusionCriteria,
    @JsonProperty("display") String display
) {

}
