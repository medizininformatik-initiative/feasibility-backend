package de.numcodex.feasibility_gui_backend.terminology.db;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;

public record Coding(String system, String code, String version) implements Serializable {
  public Coding {
    requireNonNull(system);
    requireNonNull(code);
  }
}
