package de.numcodex.feasibility_gui_backend.query.api.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@JsonInclude()
@Builder
public record QueryQuotaEntry(
    @JsonProperty("intervalInMinutes") int intervalInMinutes,
    @JsonProperty("used") int used,
    @JsonProperty("total") int limit
) {
}
