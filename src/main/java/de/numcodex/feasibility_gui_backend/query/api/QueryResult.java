package de.numcodex.feasibility_gui_backend.query.api;

import lombok.Builder;

import java.util.List;


@Builder
public record QueryResult(
    long totalNumberOfPatients,
    Long queryId,
    List<QueryResultLine> resultLines
) {

}
