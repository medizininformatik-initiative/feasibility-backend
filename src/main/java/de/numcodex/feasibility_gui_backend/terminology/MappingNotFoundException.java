package de.numcodex.feasibility_gui_backend.terminology;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class MappingNotFoundException extends RuntimeException {

  public MappingNotFoundException() {
  }
}
