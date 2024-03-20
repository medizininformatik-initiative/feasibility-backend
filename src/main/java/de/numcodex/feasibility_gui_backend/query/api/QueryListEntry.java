package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import lombok.Builder;

import java.sql.Timestamp;
import java.util.List;

@JsonInclude(Include.NON_NULL)
@Builder
public record QueryListEntry(
    @JsonProperty long id,
    @JsonProperty String label,
    @JsonProperty String comment,
    @JsonProperty Timestamp createdAt,
    @JsonProperty Long totalNumberOfPatients,
    @JsonProperty List<Criterion> invalidCriteria
)  {

}
