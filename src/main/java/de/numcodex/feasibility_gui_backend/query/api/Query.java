package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import java.util.List;

@JsonInclude(Include.NON_NULL)
public record Query(
    @JsonProperty long id,
    @JsonProperty StructuredQuery content,
    @JsonProperty String label,
    @JsonProperty String comment,
    @JsonProperty List<TermCode> invalidTerms
) {

}
