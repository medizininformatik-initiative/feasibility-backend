package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Builder
@JsonInclude(Include.NON_NULL)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CriteriaProfileData {
    @JsonProperty("id")
    private String id;
    @JsonProperty("display")
    private DisplayEntry display;
    @JsonProperty("context")
    private TermCode context;
    @JsonProperty("termCodes")
    @EqualsAndHashCode.Include
    private List<TermCode> termCodes;
    @JsonProperty("uiProfile")
    private UiProfile uiProfile;
}
