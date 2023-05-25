package de.numcodex.feasibility_gui_backend.config;

import de.numcodex.feasibility_gui_backend.query.api.status.FeasibilityIssue;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class AccessDeniedExceptionHandler extends
    ResponseEntityExceptionHandler {

  @ExceptionHandler({AccessDeniedException.class})
  @ResponseBody
  public ResponseEntity<List<FeasibilityIssue>> handleAccessDeniedException(
      Exception ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(List.of(FeasibilityIssue.USER_INCORRECT_ACCESS_RIGHTS));
  }
}
