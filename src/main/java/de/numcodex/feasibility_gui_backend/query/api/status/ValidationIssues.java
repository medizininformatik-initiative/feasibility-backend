package de.numcodex.feasibility_gui_backend.query.api.status;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@JsonSerialize()
@Builder
public record ValidationIssues(
    List<ValidationIssue> issues
) {
}
