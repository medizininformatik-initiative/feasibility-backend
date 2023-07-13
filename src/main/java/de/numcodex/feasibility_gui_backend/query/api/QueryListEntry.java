package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;

@JsonInclude(Include.NON_NULL)
public record QueryListEntry(
    @JsonProperty long id,
    @JsonProperty String label,
    @JsonProperty Timestamp createdAt
)  {

}
