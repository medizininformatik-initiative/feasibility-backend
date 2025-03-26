package de.numcodex.feasibility_gui_backend.dse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Builder
public record Filter(
    @JsonProperty String type,
    @JsonProperty String name,
    @JsonProperty(value = "ui_type") String uiType,
    @JsonProperty List<String> valueSetUrls
) {
  public Filter {
    valueSetUrls = (valueSetUrls == null) ? List.of() : valueSetUrls;
  }
}
