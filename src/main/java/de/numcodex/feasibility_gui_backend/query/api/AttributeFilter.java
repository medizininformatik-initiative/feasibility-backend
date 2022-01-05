package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import lombok.Setter;

@Setter
@JsonInclude(Include.NON_NULL)
public class AttributeFilter extends ValueFilter {
    @JsonProperty(value = "attributeCode", required = true)
    private TermCode attributeCode;
}
