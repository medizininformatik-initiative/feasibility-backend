package de.numcodex.feasibility_gui_backend.model.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.model.common.TermCode;
import de.numcodex.feasibility_gui_backend.model.query.AttributeFilter;
import de.numcodex.feasibility_gui_backend.model.query.TimeRestriction;
import de.numcodex.feasibility_gui_backend.model.query.ValueFilter;
import java.util.ArrayList;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class Criterion {

  @JsonProperty("termCodes")
  private ArrayList<TermCode> termCodes;

  @JsonProperty("attributeFilters")
  private ArrayList<AttributeFilter> attributeFilters;

  @JsonProperty("valueFilter")
  private ValueFilter valueFilter;

  @JsonProperty("timeRestriction")
  private TimeRestriction timeRestriction;
}
