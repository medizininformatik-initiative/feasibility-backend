package de.numcodex.feasibility_gui_backend.dse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record Field(
    @JsonProperty String id,
    @JsonProperty DisplayEntry display,
    @JsonProperty DisplayEntry description,
    @JsonProperty String type,
    @JsonProperty boolean recommended,
    @JsonProperty boolean required,
    @JsonProperty List<Field> children
) {
  public Field {
    children = (children == null) ? List.of() : children;
  }
}
