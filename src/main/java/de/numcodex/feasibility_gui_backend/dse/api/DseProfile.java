package de.numcodex.feasibility_gui_backend.dse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record DseProfile(
    @JsonProperty String url,
    @JsonProperty DisplayEntry display,
    @JsonProperty List<Field> fields,
    @JsonProperty List<Filter> filters,
    @JsonProperty String errorCode,
    @JsonProperty String errorCause
) {

}
