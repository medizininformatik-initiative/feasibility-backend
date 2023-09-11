package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.sql.Timestamp;

@JsonInclude(Include.NON_NULL)
@Builder
public record QueryListEntry(
    @JsonProperty long id,
    @JsonProperty String label,
    @JsonProperty Timestamp createdAt,
    @JsonProperty Long totalNumberOfPatients
)  {

}
