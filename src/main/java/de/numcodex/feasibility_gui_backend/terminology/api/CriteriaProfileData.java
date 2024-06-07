package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonInclude(Include.NON_NULL)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CriteriaProfileData {
    @JsonProperty("id")
    private String id;
    @JsonProperty("context")
    private TermCode context;
    @JsonProperty("termCode")
    @EqualsAndHashCode.Include
    private TermCode termCode ;
    @JsonProperty("uiProfile")
    private UiProfile uiProfile;
}
