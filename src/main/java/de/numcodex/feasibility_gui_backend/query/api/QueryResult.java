package de.numcodex.feasibility_gui_backend.query.api;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QueryResult {
    private int totalNumberOfPatients;
    private Long queryId;
    private List<QueryResultLine> resultLines;
}
