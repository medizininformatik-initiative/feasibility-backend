package de.numcodex.feasibility_gui_backend.query.api.status;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@JsonSerialize()
public class FeasibilityIssues {

  public List<FeasibilityIssue> issues;

  public FeasibilityIssues(List<FeasibilityIssue> issues) {
    this.issues = issues;
  }

}
