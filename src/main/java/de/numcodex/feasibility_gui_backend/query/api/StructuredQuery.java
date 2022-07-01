package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.query.api.validation.StructuredQueryValidation;
import lombok.Data;

import java.net.URI;
import java.util.List;

@Data
// TODO: This most definitely should be (Include.NON_EMPTY) requires front-end changes.
// Also change accordingy in QueryHandlerRestControllerIT.java
@JsonInclude(Include.NON_NULL)
@StructuredQueryValidation
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
