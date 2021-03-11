package de.numcodex.feasibility_gui_backend.model.query;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QueryResult {
    private int totalNumberOfPatients;
    private String queryId;

    private List<QueryResultLine> resultLines = new ArrayList<>();
}
