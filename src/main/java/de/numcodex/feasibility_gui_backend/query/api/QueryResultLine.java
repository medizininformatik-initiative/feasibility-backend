package de.numcodex.feasibility_gui_backend.query.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryResultLine {
    private String siteName;
    private int numberOfPatients;
}
