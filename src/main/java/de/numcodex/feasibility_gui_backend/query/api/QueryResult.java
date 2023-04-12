package de.numcodex.feasibility_gui_backend.query.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResult {
    private Long totalNumberOfPatients;
    private Long queryId;
    private List<QueryResultLine> resultLines;
    private ResultAbsentReason absentReasonTotal;
    private ResultAbsentReason absentReasonResultLines;
}
