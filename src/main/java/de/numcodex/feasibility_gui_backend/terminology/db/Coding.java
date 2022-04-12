package de.numcodex.feasibility_gui_backend.terminology.db;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;

public class Coding implements Serializable {
  private String system;
  private String code;
  private String version;

  public Coding() {}

  public Coding(String system, String code, String version) {
    requireNonNull(system);
    requireNonNull(code);
    this.system = system;
    this.code= code;
  }
}
