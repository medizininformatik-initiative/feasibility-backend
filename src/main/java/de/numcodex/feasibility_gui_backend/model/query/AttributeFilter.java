package de.numcodex.feasibility_gui_backend.model.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.model.common.TermCode;

@JsonInclude(Include.NON_NULL)
public class AttributeFilter extends  ValueFilter{
  @JsonProperty(value = "attributeCode", required = true)
  private TermCode attributeCode;

}
