package de.numcodex.feasibility_gui_backend.dse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record DseProfile(
    @JsonProperty String url,
    @JsonProperty String display,
    @JsonProperty List<Field> fields,
    @JsonProperty List<Filter> filters,
    @JsonProperty String errorCode,
    @JsonProperty String errorCause
) {

}
