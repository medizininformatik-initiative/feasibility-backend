package de.numcodex.feasibility_gui_backend.dse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class DseProfileException extends RuntimeException {

  public DseProfileException(String message) {
    super(message);
  }
}
