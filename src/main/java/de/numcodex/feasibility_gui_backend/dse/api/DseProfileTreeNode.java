package de.numcodex.feasibility_gui_backend.dse.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record DseProfileTreeNode(
    @JsonProperty String id,
    @JsonProperty List<DseProfileTreeNode> children,
    @JsonProperty String name,
    @JsonProperty DisplayEntry display,
    @JsonProperty FieldDisplayEntry fields,
    @JsonProperty String module,
    @JsonProperty String url,
    @JsonProperty boolean leaf,
    @JsonProperty boolean selectable
) {
}
