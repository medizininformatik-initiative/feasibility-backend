package de.numcodex.feasibility_gui_backend.query.api;

import lombok.Builder;

@Builder
public record QueryResultLine(
    String siteName,
    long numberOfPatients
) {

}
