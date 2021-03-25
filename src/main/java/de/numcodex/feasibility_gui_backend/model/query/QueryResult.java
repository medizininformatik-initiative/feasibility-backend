package de.numcodex.feasibility_gui_backend.model.query;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class QueryResult {
    private int totalNumberOfPatients;
    private String queryId;

    private List<QueryResultLine> resultLines = new ArrayList<>();
}
