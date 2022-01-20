package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import lombok.Data;

import java.net.URI;
import java.util.List;

@Data
@JsonInclude(Include.NON_EMPTY)
public class StructuredQuery {

    @JsonProperty
    private URI version;
    @JsonProperty("inclusionCriteria")
    private List<List<Criterion>> inclusionCriteria;
    @JsonProperty("exclusionCriteria")
    private List<List<Criterion>> exclusionCriteria;
    @JsonProperty("display")
    private String display;
}
